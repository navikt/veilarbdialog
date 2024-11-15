package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock
import lombok.SneakyThrows
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.minsidevarsler.dto.DialogVarselStatus
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternStatusOppdatertEventName
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselHendelseDTO
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselKanal
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselStatus
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinsideVarselDao
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.util.DialogTestService
import no.nav.fo.veilarbdialog.util.KafkaTestService
import no.nav.tms.varsel.action.Varseltype
import org.apache.kafka.clients.producer.RecordMetadata
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import java.util.concurrent.ExecutionException

internal class EksternVarslingKvitteringTest(
    @Autowired
    var minsideVarslDao: MinsideVarselDao,
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
        val forhåndsVarsel = dialogTestService.startEskalering(veileder, startEskaleringDto)

        val opprinneligBrukernotifikasjon = minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarsel.id)

        val oversendtMelding = bestiltStatus(opprinneligBrukernotifikasjon.varselId)
        val oversendtRecordMetadata = sendKvitteringsMelding(oversendtMelding)
        assertExpectedBrukernotifikasjonStatus(
            forhåndsVarsel.id,
            opprinneligBrukernotifikasjon,
            oversendtRecordMetadata,
            VarselKvitteringStatus.IKKE_SATT
        )

        val ferdigstiltMelding = sendtStatus(opprinneligBrukernotifikasjon.varselId)
        val ferdigstiltRecordMetadata = sendKvitteringsMelding(ferdigstiltMelding)
        assertExpectedBrukernotifikasjonStatus(
            forhåndsVarsel.id,
            opprinneligBrukernotifikasjon,
            ferdigstiltRecordMetadata,
            VarselKvitteringStatus.OK
        )

        val feiletMelding = feiletStatus(opprinneligBrukernotifikasjon.varselId)
        val feiletRecordMetadata = sendKvitteringsMelding(feiletMelding)
        assertExpectedBrukernotifikasjonStatus(
            forhåndsVarsel.id,
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
        forhåndsVarselId: Long,
        opprinneligBrukernotifikasjon: DialogVarselStatus,
        recordMetadata: RecordMetadata,
        expectedStatus: VarselKvitteringStatus?
    ) {
        val offset = recordMetadata.offset()
        val partition = recordMetadata.partition()

        kafkaTestService.assertErKonsumertAiven(minsideVarselHendelseTopic, offset, partition, 10)

        val brukernotifikasjonEtterProsessering = minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarselId)

        SoftAssertions.assertSoftly{ assertions ->
            assertions.assertThat(brukernotifikasjonEtterProsessering.varselId.value).isEqualTo(opprinneligBrukernotifikasjon.varselId.value)
            assertions.assertThat(brukernotifikasjonEtterProsessering.kvitteringStatus).isEqualTo(expectedStatus)
            assertions.assertAll()
        }
    }

    private fun lagVarselHendelseMelding(varselId: MinSideVarselId, status: EksternVarselStatus): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            EksternStatusOppdatertEventName,
            "dab",
            appname,
            Varseltype.Beskjed,
            varselId.value,
            status,
            false,
            null,
            EksternVarselKanal.SMS
        )
    }

    private fun sendtStatus(bestillingsId: MinSideVarselId): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(bestillingsId, EksternVarselStatus.sendt)
    }

    private fun feiletStatus(bestillingsId: MinSideVarselId): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(bestillingsId, EksternVarselStatus.feilet)
    }

    private fun bestiltStatus(eventId: MinSideVarselId): EksternVarselHendelseDTO {
        return lagVarselHendelseMelding(eventId, EksternVarselStatus.bestilt)
    }
}
