package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.*
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus.*
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.VarselHendelseUtil.eksternVarselHendelseBestilt
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.VarselHendelseUtil.eksternVarselHendelseFeilet
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.VarselHendelseUtil.eksternVarselHendelseSendt
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.VarselHendelseUtil.lagInternVarselHendelseMelding
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselService
import no.nav.fo.veilarbdialog.minsidevarsler.dto.DialogVarselEntity
import no.nav.fo.veilarbdialog.minsidevarsler.dto.InternVarselHendelseType
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
    @Autowired
    var minsideVarselService: MinsideVarselService,
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
    fun skal_oppdatere_brukernotifikasjonVedKvitteringsStatusOppdateringer() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)

        val startEskaleringDto =
            StartEskaleringDto(Fnr.of(bruker.fnr), "begrunnelse", "overskrift", "henvendelseTekst", null)
        val forhåndsVarsel = dialogTestService.startEskalering(veileder, startEskaleringDto)

        var (bestiltHendelse, varselId) = minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarsel.id).let {
            eksternVarselHendelseBestilt(it.varselId, appname) to it.varselId
        }

        val bestiltRecordMetadata = publiserVarselHendelsePåKafka(bestiltHendelse)
        assertErKonsummert(bestiltRecordMetadata)
        minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarsel.id).let {
            assertExpectedVarselStatuser(
                it,
                kvitteringsStatus = IKKE_SATT,
                status = SENDT
            )
        }

        val sendtHendelse = eksternVarselHendelseSendt(varselId, appname)
        val ferdigstiltRecordMetadata = publiserVarselHendelsePåKafka(sendtHendelse)
        assertErKonsummert(ferdigstiltRecordMetadata)
        minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarsel.id).let {
            assertExpectedVarselStatuser(
                it,
                kvitteringsStatus = OK,
                status = SENDT
            )
        }


        val feiletHendelse = eksternVarselHendelseFeilet(varselId, appname)
        val feiletRecordMetadata = publiserVarselHendelsePåKafka(feiletHendelse)
        assertErKonsummert(feiletRecordMetadata)
        minsideVarslDao.getMinsideVarselForForhåndsvarsel(forhåndsVarsel.id). let {
            assertExpectedVarselStatuser(
                it,
                kvitteringsStatus = FEILET,
                status = SENDT
            )
        }


    }

    @Test
    fun skal_oppdatere_brukernotifikasjonVedVarselStatusOppdateringer() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val melding = NyMeldingDTO().setFnr(bruker.fnr).setOverskrift("Overskrift").setTekst("Tekst")
        val opprettetDialog = dialogTestService.opprettDialogSomVeileder(veileder, bruker, melding)

        var varselId = minsideVarslDao.getVarslerForDialog(opprettetDialog.id.toLong()).first().let {
            assertExpectedVarselStatuser(
                it,
                kvitteringsStatus = IKKE_SATT,
                status = PENDING
            )
            it.varselId
        }
        minsideVarselService.sendPendingVarslerCronImpl()

        minsideVarslDao.getVarslerForDialog(opprettetDialog.id.toLong()).first().let {
            assertExpectedVarselStatuser(
                it,
                kvitteringsStatus = IKKE_SATT,
                status = SENDT
            )
        }

        val internVarselHendelseMelding =
            lagInternVarselHendelseMelding(varselId, InternVarselHendelseType.opprettet, appname)
        publiserVarselHendelsePåKafka(internVarselHendelseMelding)
        minsideVarslDao.hentVarselEntity(varselId).let {
            assertExpectedVarselStatuser(
                it!!,
                kvitteringsStatus = IKKE_SATT,
                status = SENDT
            )
        }

    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun publiserVarselHendelsePåKafka(melding: TestVarselHendelseDTO?): RecordMetadata {
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
        actualVarsel: DialogVarselEntity,
        kvitteringsStatus: VarselKvitteringStatus,
        status: BrukernotifikasjonBehandlingStatus
    ) {

        assertSoftly{ assertions ->
            assertions.assertThat(actualVarsel.kvitteringStatus).isEqualTo(kvitteringsStatus)
            assertions.assertThat(actualVarsel.status).isEqualTo(status)
            assertions.assertAll()
        }
    }

}
