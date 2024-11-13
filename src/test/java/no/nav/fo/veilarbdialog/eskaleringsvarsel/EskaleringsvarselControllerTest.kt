package no.nav.fo.veilarbdialog.eskaleringsvarsel

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.restassured.response.Response
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService
import no.nav.fo.veilarbdialog.domain.DialogDTO
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.GjeldendeEskaleringsvarselDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder
import no.nav.fo.veilarbdialog.minsidevarsler.ScheduleSendBrukernotifikasjonerForUlesteDialoger
import no.nav.fo.veilarbdialog.util.DialogTestService
import no.nav.fo.veilarbdialog.util.KafkaTestService
import no.nav.tms.varsel.action.InaktiverVarsel
import no.nav.tms.varsel.action.OpprettVarsel
import no.nav.tms.varsel.action.Sensitivitet
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.lang.Exception
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

internal class EskaleringsvarselControllerTest(
    @Value("\${application.topic.ut.minside.varsel}")
    private val minsidevarselTopic: String,
    @Value("\${application.dialog.url}")
    private val dialogUrl: String,
    @Value("\${spring.application.name}")
    private val applicationName: String,
    @Value("\${application.namespace}")
    private val namespace: String,
    @Autowired
    var dialogTestService: DialogTestService,
    @Autowired
    var kafkaTestService: KafkaTestService,
    @Autowired
    var brukernotifikasjonService: BrukernotifikasjonService,
    @Autowired
    var scheduleSendBrukernotifikasjonerForUlesteDialoger: ScheduleSendBrukernotifikasjonerForUlesteDialoger,

    ) : SpringBootTestBase() {
    private val log = LoggerFactory.getLogger(EskaleringsvarselController::class.java)

    var minsideVarselConsumer: Consumer<String?, String?>? = null

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupAll() {
            JsonUtils.getMapper().registerKotlinModule()
        }
    }


    @BeforeEach
    fun setupL() {
        minsideVarselConsumer = kafkaTestService.createStringStringConsumer(minsidevarselTopic)
    }

    private fun ventPåVarselOpprettelsePåKafka(): OpprettVarsel {
        val brukernotifikasjonRecord =
            KafkaTestUtils.getSingleRecord<String, String>(
                minsideVarselConsumer,
                minsidevarselTopic,
                KafkaTestService.DEFAULT_WAIT_TIMEOUT
            )
        return JsonUtils.fromJson<OpprettVarsel>(brukernotifikasjonRecord.value(), OpprettVarsel::class.java)
    }

    private fun ventPåVarselInaktiveringPåKafka(): InaktiverVarsel {
        val brukernotifikasjonRecord =
            KafkaTestUtils.getSingleRecord<String?, String?>(
                minsideVarselConsumer,
                minsidevarselTopic,
                KafkaTestService.DEFAULT_WAIT_TIMEOUT
            )
        return JsonUtils.fromJson<InaktiverVarsel>(brukernotifikasjonRecord.value(), InaktiverVarsel::class.java)
    }


    private fun lagEskaleringsVarsel(bruker: MockBruker): StartEskaleringDto {
        return StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst", null)
    }

    @Test
    fun start_eskalering_happy_case() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val begrunnelse = "Fordi ..."
        val overskrift = "Dialog tittel"
        val henvendelseTekst = "Henvendelsestekst... lang tekst"

        // Tekst som brukes i eventet på DittNav. Påkrevd, ingen default
        val brukernotifikasjonEventTekst = "Viktig oppgave. NAV vurderer å stanse pengene dine. Se hva du må gjøre."
        // Påloggingsnivå for å lese eventet på DittNav. Dersom eventteksten er sensitiv, må denne være 4.
        val sikkerhetsNivaa =  Sensitivitet.High
        // Lenke som blir aktivert når bruker klikker på eventet
        var eventLink: String?

        val startEskaleringDto =
            StartEskaleringDto(Fnr.of(bruker.fnr), begrunnelse, overskrift, henvendelseTekst, null)
        val startEskalering = dialogTestService.startEskalering(veileder, startEskaleringDto)

        val dialogDTO = dialogTestService.hentDialog(veileder, startEskalering.tilhorendeDialogId)

        eventLink = dialogUrl + "/" + dialogDTO.id
        SoftAssertions.assertSoftly { assertions: SoftAssertions? ->
            assertions!!.assertThat(dialogDTO.isFerdigBehandlet).isTrue()
            assertions.assertThat(dialogDTO.isVenterPaSvar).isTrue()
            val henvendelseDTO = dialogDTO.henvendelser[0]
            assertions.assertThat(henvendelseDTO.tekst).isEqualTo(henvendelseTekst)
            assertions.assertThat(henvendelseDTO.avsenderId).isEqualTo(veileder.navIdent)
            assertions.assertAll()
        }


        val gjeldende = requireGjeldende(veileder, bruker)

        Assertions.assertThat(startEskalering.id).isEqualTo(gjeldende.id)
        Assertions.assertThat(startEskalering.tilhorendeDialogId).isEqualTo(gjeldende.tilhorendeDialogId)
        Assertions.assertThat(startEskalering.opprettetAv).isEqualTo(gjeldende.opprettetAv)
        Assertions.assertThat(startEskalering.opprettetDato).isEqualToIgnoringNanos(gjeldende.opprettetDato)
        Assertions.assertThat(startEskalering.opprettetBegrunnelse).isEqualTo(gjeldende.opprettetBegrunnelse)

        val opprettVarsel = ventPåVarselOpprettelsePåKafka()

        SoftAssertions.assertSoftly { assertions ->
            assertions.assertThat(opprettVarsel.ident).isEqualTo(bruker.fnr);
            assertions.assertThat(opprettVarsel.produsent.appnavn).isEqualTo(applicationName);
            assertions.assertThat(opprettVarsel.produsent.namespace).isEqualTo(namespace);
            assertions.assertThat(opprettVarsel.varselId).isNotEmpty();

            assertions.assertThat(opprettVarsel.eksternVarsling).isNotNull();
            assertions.assertThat(opprettVarsel.sensitivitet).isEqualTo(sikkerhetsNivaa);
            assertions.assertThat(opprettVarsel.link).isEqualTo(eventLink);
            assertions.assertThat(opprettVarsel.tekster.first().tekst).isEqualTo(brukernotifikasjonEventTekst);

            assertions.assertThat(opprettVarsel.eksternVarsling?.epostVarslingstittel).isNull();
            assertions.assertThat(opprettVarsel.eksternVarsling?.epostVarslingstekst).isNull();
            assertions.assertThat(opprettVarsel.eksternVarsling?.smsVarslingstekst).isNull();
            assertions.assertAll();
        };
    }

    @Test
    fun stop_eskalering_med_henvendelse() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val avsluttBegrunnelse = "Du har gjort aktiviteten vi ba om."
        val brukerFnr = Fnr.of(bruker.getFnr())

        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        val eskaleringsvarsel = dialogTestService!!.startEskalering(veileder, startEskaleringDto)

        val stopEskaleringDto = StopEskaleringDto(brukerFnr, avsluttBegrunnelse, true)
        stopEskalering(veileder, stopEskaleringDto)

        val dialogDTO = dialogTestService.hentDialog(veileder, eskaleringsvarsel.tilhorendeDialogId)

        SoftAssertions.assertSoftly { assertions: SoftAssertions? ->
            val henvendelser = dialogDTO.henvendelser
            assertions!!.assertThat<HenvendelseDTO?>(henvendelser).hasSize(2)

            val stopEskaleringHendvendelse = dialogDTO.henvendelser.get(1)
            assertions.assertThat(stopEskaleringHendvendelse.tekst).isEqualTo(avsluttBegrunnelse)
            assertions.assertThat(stopEskaleringHendvendelse.avsenderId).isEqualTo(veileder.navIdent)
        }

        ingenGjeldende(veileder, bruker)

        val inaktivering = ventPåVarselInaktiveringPåKafka()

        SoftAssertions.assertSoftly { assertions: SoftAssertions? ->
            assertions!!.assertThat(inaktivering.produsent.appnavn).isEqualTo(applicationName)
            assertions.assertThat(inaktivering.produsent.namespace).isEqualTo(namespace)
            //            assertions.assertThat(nokkelInput.getEventId()).isNotEmpty();
//            assertions.assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(doneInput.getTidspunkt()), ZoneOffset.UTC)).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(10, ChronoUnit.SECONDS));
            assertions.assertAll()
        }
    }

    @Test
    fun stop_eskalering_uten_henvendelse() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val avsluttBegrunnelse = "Fordi ..."
        val brukerFnr = Fnr.of(bruker.fnr)

        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        val eskaleringsvarsel = dialogTestService.startEskalering(veileder, startEskaleringDto)

        val stopEskaleringDto = StopEskaleringDto(brukerFnr, avsluttBegrunnelse, false)
        stopEskalering(veileder, stopEskaleringDto)

        val dialogDTO = dialogTestService.hentDialog(veileder, eskaleringsvarsel.tilhorendeDialogId)

        SoftAssertions.assertSoftly { assertions: SoftAssertions? ->
            val hendvendelser = dialogDTO.henvendelser
            assertions!!.assertThat<HenvendelseDTO?>(hendvendelser).hasSize(1)
        }

        ingenGjeldende(veileder, bruker)

        val inaktivering = ventPåVarselInaktiveringPåKafka()

        SoftAssertions.assertSoftly { assertions: SoftAssertions? ->
            assertions!!.assertThat(inaktivering.produsent.appnavn).isEqualTo(applicationName)
            assertions.assertThat(inaktivering.produsent.namespace).isEqualTo(namespace)
            //            assertions.assertThat(nokkelInput.getEventId()).isNotEmpty();
//            assertions.assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(doneInput.getTidspunkt()), ZoneOffset.UTC)).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(10, ChronoUnit.SECONDS));
            assertions.assertAll()
        }
    }

    @Test
    fun bruker_kan_ikke_varsles() {
        val bruker = MockNavService.createHappyBruker()
        val reservertKrr: BrukerOptions? = bruker.getBrukerOptions().toBuilder().erReservertKrr(true).build()
        MockNavService.updateBruker(bruker, reservertKrr)

        val veileder = MockNavService.createVeileder(bruker)

        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        val response = tryStartEskalering(veileder, startEskaleringDto)

        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value())

        ingenGjeldende(veileder, bruker)
    }

    @Test
    fun bruker_ikke_under_oppfolging() {
        val bruker = MockNavService.createHappyBruker()
        val reservertKrr: BrukerOptions? = bruker.getBrukerOptions().toBuilder().underOppfolging(false).build()
        MockNavService.updateBruker(bruker, reservertKrr)

        val veileder = MockNavService.createVeileder(bruker)

        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        val response = tryStartEskalering(veileder, startEskaleringDto)

        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value())

        ingenGjeldende(veileder, bruker)
    }

    @Test
    fun bruker_har_allerede_aktiv_eskalering() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        dialogTestService!!.startEskalering(veileder, startEskaleringDto)
        val response = tryStartEskalering(veileder, startEskaleringDto)
        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value())
    }

    @Test
    fun test_historikk() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        dialogTestService!!.startEskalering(veileder, startEskaleringDto)
        val stopEskaleringDto =
            StopEskaleringDto(Fnr.of(bruker.getFnr()), "avsluttbegrunnelse", false)
        stopEskalering(veileder, stopEskaleringDto)
        dialogTestService!!.startEskalering(veileder, startEskaleringDto)
        stopEskalering(veileder, stopEskaleringDto)
        dialogTestService!!.startEskalering(veileder, startEskaleringDto)
        stopEskalering(veileder, stopEskaleringDto)
        dialogTestService!!.startEskalering(veileder, startEskaleringDto)


        val eskaleringsvarselDtos = hentHistorikk(veileder, bruker)
        Assertions.assertThat<EskaleringsvarselDto?>(eskaleringsvarselDtos).hasSize(4)
        val eldste = eskaleringsvarselDtos!!.get(3)

        SoftAssertions.assertSoftly { assertions: SoftAssertions? ->
            assertions!!.assertThat(eldste.tilhorendeDialogId).isNotNull()
            assertions.assertThat(eldste.id).isNotNull()
            assertions.assertThat(eldste.opprettetAv).isEqualTo(veileder.getNavIdent())
            assertions.assertThat(eldste.opprettetBegrunnelse).isEqualTo("begrunnelse")
            assertions.assertThat(eldste.avsluttetBegrunnelse).isEqualTo("avsluttbegrunnelse")
            assertions.assertThat(eldste.avsluttetAv).isEqualTo(veileder.getNavIdent())
            assertions.assertThat(eldste.opprettetDato)
                .isCloseTo(ZonedDateTime.now(), Assertions.within(5, ChronoUnit.SECONDS))
            assertions.assertThat(eldste.avsluttetDato)
                .isCloseTo(ZonedDateTime.now(), Assertions.within(5, ChronoUnit.SECONDS))
        }
    }

    @Test
    @Throws(Exception::class)
    fun skal_kun_prosessere_en_ved_samtidige_kall() {
        val antallKall = 10
        val bakgrunnService = Executors.newFixedThreadPool(3)
        val latch = CountDownLatch(antallKall)

        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)

        val startEskaleringDto = lagEskaleringsVarsel(bruker)

        for (i in 0 until antallKall) {
            bakgrunnService.submit(Runnable {
                try {
                    dialogTestService!!.startEskalering(veileder, startEskaleringDto)
                } catch (e: Exception) {
                    log.warn("Feil i tråd.", e)
                } finally {
                    latch.countDown()
                }
            })
        }
        latch.await()

        requireGjeldende(veileder, bruker)

        ventPåVarselOpprettelsePåKafka()
        kafkaTestService.harKonsumertAlleMeldinger(minsidevarselTopic, minsideVarselConsumer)
    }

    @Test
    fun hentGjeldendeSomEksternbruker() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val startEskaleringDto =
            StartEskaleringDto(Fnr.of(bruker.fnr), "begrunnelse", "overskrift", "henvendelseTekst", null)
        dialogTestService.startEskalering(veileder, startEskaleringDto)

        bruker.createRequest()
            .`when`()
            .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
            .then()
            .assertThat().statusCode(HttpStatus.OK.value())
            .extract()
            .response()
    }

    @Test
    fun hentGjeldendeSomVeilederUtenFnrParam() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        dialogTestService.startEskalering(veileder, startEskaleringDto)

        veileder.createRequest()
            .`when`()
            .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
            .then()
            .assertThat().statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun send_done_naar_eskalering_lest_av_bruker() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)

        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        val startEskalering = dialogTestService!!.startEskalering(veileder, startEskaleringDto)

        lesHenvendelse(bruker, startEskalering.tilhorendeDialogId)

        val inaktivering = ventPåVarselInaktiveringPåKafka()
        //        assertThat(bruker.getFnr()).isEqualTo(nokkel.getFodselsnummer());
    }

    @Test
    @Throws(InterruptedException::class)
    fun unngaaDobleNotifikasjonerPaaEskaleringsvarsel() {
        val bruker = MockNavService.createHappyBruker()
        val veileder = MockNavService.createVeileder(bruker)
        val startEskaleringDto = lagEskaleringsVarsel(bruker)
        val initialEndOffsets =
            KafkaTestUtils.getEndOffsets(minsideVarselConsumer, minsidevarselTopic)

        dialogTestService.startEskalering(veileder, startEskaleringDto)

        Thread.sleep(2000L)
        // Batchen bestiller beskjeder ved nye dialoger (etter 1000 ms)
        scheduleSendBrukernotifikasjonerForUlesteDialoger.sendBrukernotifikasjonerForUlesteDialoger()
        brukernotifikasjonService.sendPendingVarsler()

        // sjekk at det er blitt sendt en oppgave
        ventPåVarselOpprettelsePåKafka()

        val afterOffsets = KafkaTestUtils.getEndOffsets(minsideVarselConsumer, minsidevarselTopic)
        // sjekk at det ikke ble sendt mer enn 1 varsel
        assertEquals(
            initialEndOffsets.entries.first().value + 1,
            afterOffsets.entries.first().value
        )
    }

    private fun hentHistorikk(veileder: MockVeileder, mockBruker: MockBruker): MutableList<EskaleringsvarselDto>? {
        return veileder.createRequest()
            .param("fnr", mockBruker.getFnr())
            .`when`()
            .get("/veilarbdialog/api/eskaleringsvarsel/historikk")
            .then()
            .assertThat().statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList<EskaleringsvarselDto?>(".", EskaleringsvarselDto::class.java)
    }

    private fun tryStartEskalering(veileder: MockVeileder, startEskaleringDto: StartEskaleringDto?): Response {
        val response = veileder.createRequest()
            .body(startEskaleringDto)
            .`when`()
            .post("/veilarbdialog/api/eskaleringsvarsel/start")
            .then()
            .extract().response()
        brukernotifikasjonService.sendPendingVarsler()
        return response
    }

    private fun stopEskalering(veileder: MockVeileder, stopEskaleringDto: StopEskaleringDto?) {
        dialogTestService.stoppEskalering(veileder, stopEskaleringDto)
    }

    private fun lesHenvendelse(bruker: MockBruker, dialogId: Long) {
        bruker.createRequest()
            .put("/veilarbdialog/api/dialog/{dialogId}/les", dialogId)
            .then()
            .assertThat().statusCode(HttpStatus.OK.value())
            .extract()
            .response()
            .`as`<DialogDTO?>(DialogDTO::class.java)
        brukernotifikasjonService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes()
    }

    private fun requireGjeldende(veileder: MockVeileder, mockBruker: MockBruker): GjeldendeEskaleringsvarselDto {
        val response = veileder.createRequest()
            .param("fnr", mockBruker.fnr)
            .`when`()
            .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
            .then()
            .assertThat().statusCode(HttpStatus.OK.value())
            .extract()
            .response()

        return response.`as`<GjeldendeEskaleringsvarselDto>(GjeldendeEskaleringsvarselDto::class.java)
    }


    private fun ingenGjeldende(veileder: MockVeileder, mockBruker: MockBruker) {
        veileder.createRequest()
            .param("fnr", mockBruker.fnr)
            .`when`()
            .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
            .then()
            .assertThat().statusCode(HttpStatus.NO_CONTENT.value())
            .extract()
            .response()
    }
}
