package no.nav.fo.veilarbdialog.rest

import no.nav.common.json.JsonUtils
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst
import no.nav.fo.veilarbdialog.domain.DialogDTO
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
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
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.SENDT
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst.NY_MELDING_TEKST
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType.BESKJED
import no.nav.fo.veilarbdialog.brukernotifikasjon.MinsideVarselService
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder
import no.nav.tms.varsel.action.InaktiverVarsel
import no.nav.tms.varsel.action.Varseltype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.temporal.ChronoUnit
import java.util.Date

internal class DialogBeskjedTest(
    @Autowired
    var brukernotifikasjonRepository: BrukernotifikasjonRepository,
    @Autowired
    var kafkaTestService: KafkaTestService,
    @Autowired
    var minsideVarselService: MinsideVarselService,
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

    private fun assertOpprettetVarselPublisertPåKafka(): OpprettVarsel {
        val brukernotifikasjonRecord =
            KafkaTestUtils.getSingleRecord<String, String>(
                minsideVarselConsumer,
                minsideVarselTopic,
                KafkaTestService.DEFAULT_WAIT_TIMEOUT
            )
        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )
        val opprettVarsel =
            JsonUtils.fromJson<OpprettVarsel>(brukernotifikasjonRecord.value(), OpprettVarsel::class.java)
        return opprettVarsel
    }

    private fun assertInaktiveringPublisertPåKafka(): InaktiverVarsel {
        val doneRecord = KafkaTestUtils.getSingleRecord<String, String>(
                minsideVarselConsumer,
                minsideVarselTopic,
                KafkaTestService.DEFAULT_WAIT_TIMEOUT
            )
        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )
        return JsonUtils.fromJson(doneRecord.value(), InaktiverVarsel::class.java)
    }

    @Test
    fun `varsel skal opprettes når veileder sender melding og inaktiveres når bruker leser melding`() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.sendEnMelding(mockBruker)

        minsideVarselService.sendPendingVarslerCron()
        val opprettVarsel = assertOpprettetVarselPublisertPåKafka()

        assertThat(opprettVarsel.tekster.first().tekst).isEqualTo(NY_MELDING_TEKST)
        assertThat(opprettVarsel.type).isEqualTo(Varseltype.Beskjed)

        val brukernotifikasjonEntity = brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(
            dialog.id.toLong(),
            BESKJED
        )[0]

        assertSoftly { assertions ->
            assertions.assertThat(brukernotifikasjonEntity.dialogId).isEqualTo(dialog.id.toLong())
            assertions.assertThat(brukernotifikasjonEntity.status).isEqualTo(SENDT)
        }

        mockBruker.lesMelding(dialog.id)
        minsideVarselService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()

        assertInaktiveringPublisertPåKafka()
        Assertions.assertThat(mockBruker.fnr).isEqualTo(opprettVarsel.ident)
    }

    private fun MockVeileder.sendEnMelding(mockBruker: MockBruker, dialogId: String? = null): DialogDTO {
        return this.createRequest()
            .body(NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift").setDialogId(dialogId))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", mockBruker.fnr)
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)
    }

    private fun MockBruker.lesMelding(dialogId: String) {
        this.createRequest()
            .put("/veilarbdialog/api/dialog/{dialogId}/les", dialogId)
            .then()
            .statusCode(200)
    }

    @Test
    fun sending_av_melding_skal_putte_varsel_i_pending() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        mockVeileder.sendEnMelding(mockBruker)
        minsideVarselService.sendPendingVarslerCron()

        val varselOpprettelse = assertOpprettetVarselPublisertPåKafka()
        assertThat(varselOpprettelse.eksternVarsling?.kanBatches).isTrue()
    }

    @Test
    fun kan_ikke_varsles() {
        val bruker = MockNavService.createBruker(BrukerOptions.happyBruker().toBuilder().erReservertKrr(true).build())
        val veileder = MockNavService.createVeileder(bruker)

        veileder.createRequest()
            .body(NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift"))
            .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
            .then()
            .statusCode(409)
    }

    @Test
    fun ikkeDobleNotifikasjonerPaaSammeDialog() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.sendEnMelding(mockBruker)
        // Hy henvendelse samme dialog
        mockVeileder.sendEnMelding(mockBruker, dialog.id)

        minsideVarselService.sendPendingVarslerCron()
        val opprettVarsel = assertOpprettetVarselPublisertPåKafka()
        assertThat(opprettVarsel.tekster.first().tekst).isEqualTo(NY_MELDING_TEKST)

        val brukernotifikasjonEntity =
            brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(
                dialog.id.toLong(),
                BESKJED
            )[0]

        assertSoftly {
            assertions: SoftAssertions ->
            assertions.assertThat(brukernotifikasjonEntity.dialogId).isEqualTo(dialog.id.toLong())
            assertions.assertThat(brukernotifikasjonEntity.type).isEqualTo(BESKJED)
            assertions.assertThat(brukernotifikasjonEntity.status).isEqualTo(SENDT)
        }

        mockBruker.lesMelding(dialog.id)

        minsideVarselService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()
        val inaktivering = assertInaktiveringPublisertPåKafka()
        assertThat(opprettVarsel.varselId).isEqualTo(inaktivering.varselId)
    }

    @Test
    fun ingenDobleNotifikasjonerPaaSammeDialogMedIntervall() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        // Send 2 meldinger på samme dialog
        val dialog = mockVeileder.sendEnMelding(mockBruker)
        mockVeileder.sendEnMelding(mockBruker, dialogId = dialog.id)

        minsideVarselService.sendPendingVarslerCron()
        assertOpprettetVarselPublisertPåKafka()
    }

    @Test
    fun `les skal inaktivere varsel selvom varsel ikke ble publisert på kafka`() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.sendEnMelding(mockBruker) // PENDING
        mockBruker.lesMelding(dialog.id) // PENDING -> SKAL_AVSLUTTES
        minsideVarselService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes() // SKAL_AVSLUTTES -> AVSLUTTET

        assertInaktiveringPublisertPåKafka()
    }

    @Test
    @Throws(InterruptedException::class)
    fun `flere meldinger på samme dialog skal ikke sende flere notifikasjoner hvis det er under 30 min siden forrige varsel`() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.sendEnMelding(mockBruker)
        minsideVarselService.sendPendingVarslerCron()

        val opprettVarsel = assertOpprettetVarselPublisertPåKafka()
        assertThat(opprettVarsel.tekster.first().tekst).isEqualTo(NY_MELDING_TEKST)

        // Hy henvendelse samme dialog
        mockVeileder.sendEnMelding(mockBruker, dialog.id)
        minsideVarselService.sendPendingVarslerCron()

        assertTrue(
            kafkaTestService.harKonsumertAlleMeldinger(
                minsideVarselTopic,
                minsideVarselConsumer,
            )
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun `flere meldinger på samme dialog skal sende flere notifikasjoner hvis det er overt 30 min siden forrige varsel`() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.sendEnMelding(mockBruker)
        settBrukernotifikasjonOpprettetForNMinuttSiden(dialog.id, 31)
        minsideVarselService.sendPendingVarslerCron()

        val opprettVarsel = assertOpprettetVarselPublisertPåKafka()
        assertThat(opprettVarsel.tekster.first().tekst).isEqualTo(NY_MELDING_TEKST)

        // Hy henvendelse samme dialog
        mockVeileder.sendEnMelding(mockBruker, dialog.id)
        minsideVarselService.sendPendingVarslerCron()

        assertOpprettetVarselPublisertPåKafka()
    }

    @Test
    @Throws(InterruptedException::class)
    fun `flere meldinger på samme dialog når første er lest skal sende notifikasjon selvom det er under 30 min siden`() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockVeileder = MockNavService.createVeileder(mockBruker)

        val dialog = mockVeileder.sendEnMelding(mockBruker)
        settBrukernotifikasjonOpprettetForNMinuttSiden(dialog.id, 1)
        minsideVarselService.sendPendingVarslerCron()

        val opprettVarsel = assertOpprettetVarselPublisertPåKafka()
        assertThat(opprettVarsel.tekster.first().tekst).isEqualTo(NY_MELDING_TEKST)

        mockBruker.lesMelding(dialog.id)
        minsideVarselService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()

        assertInaktiveringPublisertPåKafka()

        // Hy henvendelse samme dialog
        mockVeileder.sendEnMelding(mockBruker, dialog.id)
        minsideVarselService.sendPendingVarslerCron()

        assertOpprettetVarselPublisertPåKafka()
    }

    private fun settBrukernotifikasjonOpprettetForNMinuttSiden(henvendelseId: String?, minutterSiden: Int) {
        val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
        val justertOpprettet = Date.from(Instant.now().minus(minutterSiden.toLong(), ChronoUnit.MINUTES))
        val update = namedJdbcTemplate.update(
            """
                UPDATE BRUKERNOTIFIKASJON 
                SET opprettet = :sendt
                WHERE DIALOG_ID = :henvendelseId
                        
                        """.trimIndent(),
            MapSqlParameterSource("sendt", justertOpprettet)
                .addValue("henvendelseId", henvendelseId, Types.BIGINT)
        )
        assertThat(update).isEqualTo(1)
    }
}