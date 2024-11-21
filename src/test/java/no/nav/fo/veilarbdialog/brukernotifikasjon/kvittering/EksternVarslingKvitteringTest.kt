package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.SENDT
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus.FEILET
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus.OK
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus.IKKE_SATT
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.EksternVarselHendelseUtil.eksternVarselHendelseBestilt
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.EksternVarselHendelseUtil.eksternVarselHendelseFeilet
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.EksternVarselHendelseUtil.eksternVarselHendelseSendt
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.minsidevarsler.dto.DialogVarselStatus
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselHendelseDTO
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinsideVarselDao
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.util.DialogTestService
import no.nav.fo.veilarbdialog.util.KafkaTestService
import org.apache.kafka.clients.producer.RecordMetadata
import org.assertj.core.api.SoftAssertions.assertSoftly
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
    var minsideVarselHendelseProducer: KafkaTemplate<String?, String?>,
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

    @Test
    fun skal_oppdatere_brukernotifikasjon() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)

        val startEskaleringDto =
            StartEskaleringDto(Fnr.of(bruker.fnr), "begrunnelse", "overskrift", "henvendelseTekst", null)
        val forhåndsVarsel = dialogTestService.startEskalering(veileder, startEskaleringDto)

        val opprinneligBrukernotifikasjon = minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarsel.id)

        val bestiltHendelse = eksternVarselHendelseBestilt(opprinneligBrukernotifikasjon.varselId, appname)
        val bestiltRecordMetadata = publiserVarselHendelsePåKafka(bestiltHendelse)
        assertErKonsummert(bestiltRecordMetadata)
        assertExpectedVarselStatuser(
            forhåndsVarsel.id,
            opprinneligBrukernotifikasjon,
            IKKE_SATT,
            SENDT
        )

        val sendtHendelse = eksternVarselHendelseSendt(opprinneligBrukernotifikasjon.varselId, appname)
        val ferdigstiltRecordMetadata = publiserVarselHendelsePåKafka(sendtHendelse)
        assertErKonsummert(ferdigstiltRecordMetadata)
        assertExpectedVarselStatuser(
            forhåndsVarsel.id,
            opprinneligBrukernotifikasjon,
            OK,
            SENDT
        )

        val feiletHendelse = eksternVarselHendelseFeilet(opprinneligBrukernotifikasjon.varselId, appname)
        val feiletRecordMetadata = publiserVarselHendelsePåKafka(feiletHendelse)
        assertErKonsummert(feiletRecordMetadata)
        assertExpectedVarselStatuser(
            forhåndsVarsel.id,
            opprinneligBrukernotifikasjon,
            FEILET,
            SENDT
        )

    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun publiserVarselHendelsePåKafka(melding: EksternVarselHendelseDTO?): RecordMetadata {
        val send = minsideVarselHendelseProducer.send(minsideVarselHendelseTopic, JsonUtils.toJson(melding))
        minsideVarselHendelseProducer.flush()

        return send.get()!!.recordMetadata
    }

    private fun assertErKonsummert(
        recordMetadata: RecordMetadata,
    ) {
        val offset = recordMetadata.offset()
        val partition = recordMetadata.partition()
        kafkaTestService.assertErKonsumertAiven(minsideVarselHendelseTopic, offset, partition, 10)
    }

    private fun assertExpectedVarselStatuser(
        forhåndsVarselId: Long,
        opprinneligBrukernotifikasjon: DialogVarselStatus,
        kvitteringsStatus: VarselKvitteringStatus,
        status: BrukernotifikasjonBehandlingStatus
        ) {
        val varselEtterProsessering = minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarselId)
        assertSoftly{ assertions ->
            assertions.assertThat(varselEtterProsessering.varselId.value).isEqualTo(opprinneligBrukernotifikasjon.varselId.value)
            assertions.assertThat(varselEtterProsessering.kvitteringStatus).isEqualTo(kvitteringsStatus)
            assertions.assertThat(varselEtterProsessering.status).isEqualTo(status)
            assertions.assertAll()
        }
    }

}
