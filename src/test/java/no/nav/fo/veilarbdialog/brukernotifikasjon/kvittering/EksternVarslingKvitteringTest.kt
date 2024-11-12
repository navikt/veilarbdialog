package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock
import lombok.SneakyThrows
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternStatusOppdatertEventName
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselHendelseDTO
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselKanal
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselStatus
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.util.DialogTestService
import no.nav.fo.veilarbdialog.util.KafkaTestService
import no.nav.tms.varsel.action.Varseltype
import org.apache.kafka.clients.producer.RecordMetadata
import org.assertj.core.api.SoftAssertions
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.function.Consumer

internal class EksternVarslingKvitteringTest(
    @Autowired
    var brukernotifikasjonRepository: BrukernotifikasjonRepository,
    @Autowired
    var kafkaTestService: KafkaTestService,
    @Autowired
    var dialogTestService: DialogTestService,
    @Value("\${application.topic.inn.minside.varsel-hendelse}")
    var minsideVarselHendelseTopic: String,
    @Value("\${spring.application.name}")
    var appname: String,
    @Autowired
    var kvitteringsProducer: KafkaTemplate<String?, String?>,
) : SpringBootTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupAll() {
            JsonUtils.getMapper().registerKotlinModule()
        }
    }

    @AfterEach
    fun assertNoUnkowns() {
        Assertions.assertTrue(WireMock.findUnmatchedRequests().isEmpty())
    }

    @SneakyThrows
    @Test
    fun skal_oppdatere_brukernotifikasjon() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)

        val startEskaleringDto =
            StartEskaleringDto(Fnr.of(bruker.fnr), "begrunnelse", "overskrift", "henvendelseTekst", null)
        val startEskalering = dialogTestService.startEskalering(veileder, startEskaleringDto)

        val opprinneligBrukernotifikasjon = brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(
            startEskalering.tilhorendeDialogId,
            BrukernotifikasjonsType.OPPGAVE
        ).first()

        val oversendtMelding = bestiltStatus(opprinneligBrukernotifikasjon.varselId)
        val oversendtRecordMetadata = sendKvitteringsMelding(oversendtMelding)
        assertExpectedBrukernotifikasjonStatus(
            startEskalering.tilhorendeDialogId,
            opprinneligBrukernotifikasjon,
            oversendtRecordMetadata,
            VarselKvitteringStatus.IKKE_SATT
        )

        val ferdigstiltMelding = sendtStatus(opprinneligBrukernotifikasjon.varselId)
        val ferdigstiltRecordMetadata = sendKvitteringsMelding(ferdigstiltMelding)
        assertExpectedBrukernotifikasjonStatus(
            startEskalering.tilhorendeDialogId,
            opprinneligBrukernotifikasjon,
            ferdigstiltRecordMetadata,
            VarselKvitteringStatus.OK
        )

        val feiletMelding = feiletStatus(opprinneligBrukernotifikasjon.varselId)
        val feiletRecordMetadata = sendKvitteringsMelding(feiletMelding)
        assertExpectedBrukernotifikasjonStatus(
            startEskalering.tilhorendeDialogId,
            opprinneligBrukernotifikasjon,
            feiletRecordMetadata,
            VarselKvitteringStatus.FEILET
        )
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun sendKvitteringsMelding(melding: EksternVarselHendelseDTO?): RecordMetadata {
        val send = kvitteringsProducer.send(minsideVarselHendelseTopic, JsonUtils.toJson(melding))
        kvitteringsProducer.flush()

        return send.get()!!.recordMetadata
    }

    private fun assertExpectedBrukernotifikasjonStatus(
        dialogId: Long,
        opprinneligBrukernotifikasjon: BrukernotifikasjonEntity,
        recordMetadata: RecordMetadata,
        expectedStatus: VarselKvitteringStatus?
    ) {
        val offset = recordMetadata.offset()
        val partition = recordMetadata.partition()

        kafkaTestService.assertErKonsumertAiven(minsideVarselHendelseTopic, offset, partition, 10)

        val brukernotifikasjonEtterProsessering =
            brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(dialogId, BrukernotifikasjonsType.OPPGAVE)[0]

        SoftAssertions.assertSoftly(Consumer { assertions: SoftAssertions? ->
            assertions!!.assertThat<UUID?>(brukernotifikasjonEtterProsessering.varselId)
                .isEqualTo(opprinneligBrukernotifikasjon.varselId)
            assertions.assertThat<VarselKvitteringStatus?>(brukernotifikasjonEtterProsessering.varselKvitteringStatus)
                .isEqualTo(expectedStatus)
            assertions.assertAll()
        })
    }

    private fun lagVarselHendelseMelding(varselId: UUID, status: EksternVarselStatus): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            EksternStatusOppdatertEventName,
            "dab",
            appname,
            Varseltype.Beskjed,
            varselId,
            status,
            false,
            null,
            EksternVarselKanal.SMS
        )
    }

    private fun sendtStatus(bestillingsId: UUID): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(bestillingsId, EksternVarselStatus.sendt)
    }

    private fun feiletStatus(bestillingsId: UUID): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(bestillingsId, EksternVarselStatus.feilet)
    }

    private fun bestiltStatus(eventId: UUID): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(eventId, EksternVarselStatus.bestilt)
    }
}
