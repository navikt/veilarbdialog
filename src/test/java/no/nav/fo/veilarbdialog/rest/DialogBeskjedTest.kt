package no.nav.fo.veilarbdialog.rest

import no.nav.common.json.JsonUtils
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.domain.DialogDTO
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.minsidevarsler.ScheduleSendBrukernotifikasjonerForUlesteDialoger
import no.nav.fo.veilarbdialog.util.KafkaTestService
import no.nav.tms.varsel.action.OpprettVarsel
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.sql.Types
import java.time.Instant
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tms.varsel.action.InaktiverVarsel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.temporal.ChronoUnit
import java.util.Date

internal class DialogBeskjedTest(
    @Autowired
    var scheduleRessurs: ScheduleSendBrukernotifikasjonerForUlesteDialoger,
    @Autowired
    var brukernotifikasjonRepository: BrukernotifikasjonRepository,
    @Autowired
    var kafkaTestService: KafkaTestService,
    @Autowired
    var brukernotifikasjonService: BrukernotifikasjonService,
    @Value("\${application.topic.ut.minside.varsel}")
    private val minsideVarselTopic: String,
) : SpringBootTestBase() {

    init {
        JsonUtils.getMapper().registerKotlinModule()
    }

    var minsideVarselConsumer: Consumer<String, String>? = null

    @BeforeEach
    fun setupL() {
        minsideVarselConsumer = kafkaTestService.createStringStringConsumer(minsideVarselTopic)
    }

    private fun hentOpprettetVarselKafkaTopic(): OpprettVarsel {
        val brukernotifikasjonRecord =
            KafkaTestUtils.getSingleRecord<String, String>(
                minsideVarselConsumer,
                minsideVarselTopic,
                KafkaTestService.DEFAULT_WAIT_TIMEOUT
            )

        val opprettVarsel =
            JsonUtils.fromJson<OpprettVarsel>(brukernotifikasjonRecord.value(), OpprettVarsel::class.java)
        return opprettVarsel
    }

    private fun hentInaktiveringFraKafka(): InaktiverVarsel {
        val doneRecord = KafkaTestUtils.getSingleRecord<String, String>(
                minsideVarselConsumer,
                minsideVarselTopic,
                KafkaTestService.DEFAULT_WAIT_TIMEOUT
            )
        return JsonUtils.fromJson(doneRecord.value(), InaktiverVarsel::class.java)
    }

    @Test
    fun beskjed_happy_case() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.fnr)
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)

        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(dialog.henvendelser[0].id, 1)

        scheduleRessurs!!.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService!!.sendPendingVarsler()

        val opprettVarsel = hentOpprettetVarselKafkaTopic()

        //        assertThat(opprettVarsel.getTekster().getFirst().)
//                .isEqualTo(BrukernotifikasjonTekst.BESKJED_BRUKERNOTIFIKASJON_TEKST);
        val brukernotifikasjonEntity = brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(
            dialog.id.toLong(),
            BrukernotifikasjonsType.BESKJED
        )[0]

        assertSoftly {
            assertions: SoftAssertions? ->
            assertions!!.assertThat(brukernotifikasjonEntity.dialogId).isEqualTo(dialog.getId().toLong())
            assertions.assertThat<BrukernotifikasjonsType?>(brukernotifikasjonEntity.type)
                .isEqualTo(BrukernotifikasjonsType.BESKJED)
            assertions.assertThat<BrukernotifikasjonBehandlingStatus?>(brukernotifikasjonEntity.status)
                .isEqualTo(BrukernotifikasjonBehandlingStatus.SENDT)
        }

        mockBruker.createRequest()
            .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.getId())
            .then()
            .statusCode(200)

        brukernotifikasjonService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()

        hentInaktiveringFraKafka()

        Assertions.assertThat(mockBruker.fnr).isEqualTo(opprettVarsel.ident)
    }

    @Test
    fun ikke_beskjed_foer_grace_periode_utlopt() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.getFnr())
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO?>(DialogDTO::class.java)

        scheduleRessurs!!.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService!!.sendPendingVarsler()

        val harKonsumertAlleMeldinger = kafkaTestService.harKonsumertAlleMeldinger(
            minsideVarselTopic,
            minsideVarselConsumer
        )
        Assertions.assertThat(harKonsumertAlleMeldinger).isTrue()
    }

    @Test
    fun ikke_beskjed_naar_maxalder_er_passert() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.getFnr())
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)

        // Setter sendt til å være 3 sekund tidligere pga. max alder
        settHenvendelseSendtForNSekundSiden(dialog.getHenvendelser().get(0).getId(), 3)

        scheduleRessurs!!.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService!!.sendPendingVarsler()

        val harKonsumertAlleMeldinger = kafkaTestService!!.harKonsumertAlleMeldinger(
            minsideVarselTopic,
            minsideVarselConsumer
        )
        Assertions.assertThat(harKonsumertAlleMeldinger).isTrue()
    }

    @Test
    fun kan_ikke_varsles() {
        val bruker = MockNavService.createHappyBruker()
        val reservertKrr: BrukerOptions? = bruker.getBrukerOptions().toBuilder().erReservertKrr(true).build()
        MockNavService.updateBruker(bruker, reservertKrr)

        val veileder = MockNavService.createVeileder(bruker)

        veileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
            .then()
            .statusCode(409)
    }

    @Test
    fun ikkeDobleNotifikasjonerPaaSammeDialog() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.fnr)
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)

        // Hy henvendelse samme dialog
        val sammeDialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst2").setDialogId(dialog.id))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.fnr)
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)

        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(sammeDialog.henvendelser[0].id, 1)
        settHenvendelseSendtForNSekundSiden(sammeDialog.henvendelser[1].id, 1)

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService.sendPendingVarsler()

        val opprettVarsel = hentOpprettetVarselKafkaTopic()

        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer
            )
        )

        assertThat(opprettVarsel.tekster.first().tekst)
            .isEqualTo(BrukernotifikasjonTekst.NY_MELDING_TEKST)

        val brukernotifikasjonEntity =
            brukernotifikasjonRepository!!.hentBrukernotifikasjonForDialogId(
                dialog.getId().toLong(),
                BrukernotifikasjonsType.BESKJED
            ).get(0)

        assertSoftly {
            assertions: SoftAssertions? ->
            assertions!!.assertThat(brukernotifikasjonEntity.dialogId).isEqualTo(dialog.id.toLong())
            assertions.assertThat<BrukernotifikasjonsType?>(brukernotifikasjonEntity.type)
                .isEqualTo(BrukernotifikasjonsType.BESKJED)
            assertions.assertThat<BrukernotifikasjonBehandlingStatus?>(brukernotifikasjonEntity.status)
                .isEqualTo(BrukernotifikasjonBehandlingStatus.SENDT)
        }

        mockBruker.createRequest()
            .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.id)
            .then()
            .statusCode(200)

        brukernotifikasjonService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()

        val doneRecord = hentInaktiveringFraKafka()

        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer
            )
        )

        assertThat(opprettVarsel.varselId).isEqualTo(doneRecord.varselId)
    }

    @Test
    fun ingenDobleNotifikasjonerPaaSammeDialogMedIntervall() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.getFnr())
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)


        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(dialog.getHenvendelser().get(0).getId(), 1)

        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService.sendPendingVarsler()

        val opprettVarsel = hentOpprettetVarselKafkaTopic()

        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )

        assertThat(opprettVarsel.tekster.first().tekst)
            .isEqualTo(BrukernotifikasjonTekst.NY_MELDING_TEKST)

        // Hy henvendelse samme dialog
        val sammedialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst2").setDialogId(dialog.getId()))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.getFnr())
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)


        // Setter sendt til å være 1 sekund tidligere pga. grace period
        settHenvendelseSendtForNSekundSiden(sammedialog.henvendelser[1].id, 1)

        scheduleRessurs!!.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService!!.sendPendingVarsler()

        // Ingen nye beskjeder siden forrige henvendelse ikke er lest
        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )

        mockBruker.createRequest()
            .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.getId())
            .then()
            .statusCode(200)

        brukernotifikasjonService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()

        val doneRecord = hentInaktiveringFraKafka()

        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )

        assertThat(opprettVarsel.varselId).isEqualTo(doneRecord.varselId)
    }


    @Test
    @Throws(InterruptedException::class)
    fun nyNotifikasjonPaaSammeDialogNaarFoersteErLest() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.getFnr())
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)

        settHenvendelseSendtForNSekundSiden(dialog.henvendelser[0].id, 1)
        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService.sendPendingVarsler()

        val opprettVarsel = hentOpprettetVarselKafkaTopic()

        assertTrue(
            kafkaTestService!!.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )

        assertThat(opprettVarsel.tekster.first().tekst)
            .isEqualTo(BrukernotifikasjonTekst.NY_MELDING_TEKST)

        mockBruker.createRequest()
            .put("/veilarbdialog/api/dialog/{dialogId}/les", dialog.getId())
            .then()
            .statusCode(200)

        brukernotifikasjonService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()

        hentInaktiveringFraKafka()

        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )

        // Hy henvendelse samme dialog
        val sammeDialog = mockVeileder.createRequest()
            .body(NyHenvendelseDTO().setTekst("tekst2").setDialogId(dialog.id))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.fnr)
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO?>(DialogDTO::class.java)

        Thread.sleep(1000L)
        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService.sendPendingVarsler()

        // Ny beskjed siden forrige henvendelse er lest
        org.junit.jupiter.api.Assertions.assertFalse(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer
            )
        )
    }

    // Setter sendt til å være N sekund tidligere pga. grace period
    private fun settHenvendelseSendtForNSekundSiden(henvendelseId: String?, sekunderSiden: Int) {
        val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
        val nySendt = Date.from(Instant.now().minus(sekunderSiden.toLong(), ChronoUnit.SECONDS))
        val update = namedJdbcTemplate.update(
            """
                                UPDATE HENVENDELSE 
                                SET SENDT = :sendt
                                WHERE HENVENDELSE_ID = :henvendelseId
                        
                        """.trimIndent(),
            MapSqlParameterSource("sendt", nySendt)
                .addValue("henvendelseId", henvendelseId, Types.BIGINT)
        )
        assertThat(update).isEqualTo(1)
    }
}
