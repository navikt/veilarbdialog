package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.GjeldendeEskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.service.ScheduleRessurs;
import no.nav.fo.veilarbdialog.util.DialogTestService;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.nav.fo.veilarbdialog.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
class EskaleringsvarselControllerTest extends SpringBootTestBase {

    @Value("${application.topic.ut.brukernotifikasjon.oppgave}")
    private String brukernotifikasjonOppgaveTopic;

    @Value("${application.topic.ut.brukernotifikasjon.beskjed}")
    private String brukernotifikasjonBeskjedTopic;

    @Value("${application.topic.ut.brukernotifikasjon.done}")
    private String brukernotifikasjonDoneTopic;

    @Value("${application.dialog.url}")
    private String dialogUrl;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.namespace}")
    private String namespace;

    @Autowired
    DialogTestService dialogTestService;

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    ScheduleRessurs scheduleRessurs;


    Consumer<NokkelInput, OppgaveInput> brukerNotifikasjonOppgaveConsumer;

    Consumer<NokkelInput, BeskjedInput> brukerNotifikasjonBeskjedConsumer;

    Consumer<NokkelInput, DoneInput> brukerNotifikasjonDoneConsumer;

    @BeforeEach
    void setupL() {
        brukerNotifikasjonOppgaveConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonOppgaveTopic);
        brukerNotifikasjonBeskjedConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonBeskjedTopic);
        brukerNotifikasjonDoneConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonDoneTopic);
    }


    @Test
    void start_eskalering_happy_case() {

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String begrunnelse = "Fordi ...";
        String overskrift = "Dialog tittel";
        String henvendelseTekst = "Henvendelsestekst... lang tekst";

        // Tekst som brukes i eventet på DittNav. Påkrevd, ingen default
        String brukernotifikasjonEventTekst = "Viktig oppgave. NAV vurderer å stanse pengene dine. Se hva du må gjøre.";
        // Påloggingsnivå for å lese eventet på DittNav. Dersom eventteksten er sensitiv, må denne være 4.
        int sikkerhetsNivaa = 4;
        // Lenke som blir aktivert når bruker klikker på eventet
        String eventLink;

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), begrunnelse, overskrift, henvendelseTekst);
        EskaleringsvarselDto startEskalering = dialogTestService.startEskalering(veileder, startEskaleringDto);


        DialogDTO dialogDTO = dialogTestService.hentDialog(veileder, startEskalering.tilhorendeDialogId());

        eventLink = dialogUrl + "/" + dialogDTO.getId();
        SoftAssertions.assertSoftly(
                assertions -> {
                    assertions.assertThat(dialogDTO.isFerdigBehandlet()).isTrue();
                    assertions.assertThat(dialogDTO.isVenterPaSvar()).isTrue();
                    HenvendelseDTO henvendelseDTO = dialogDTO.getHenvendelser().get(0);
                    assertions.assertThat(henvendelseDTO.getTekst()).isEqualTo(henvendelseTekst);
                    assertions.assertThat(henvendelseDTO.getAvsenderId()).isEqualTo(veileder.getNavIdent());
                    assertions.assertAll();
                }
        );

        GjeldendeEskaleringsvarselDto gjeldende = requireGjeldende(veileder, bruker);

        assertThat(startEskalering.id()).isEqualTo(gjeldende.id());
        assertThat(startEskalering.tilhorendeDialogId()).isEqualTo(gjeldende.tilhorendeDialogId());
        assertThat(startEskalering.opprettetAv()).isEqualTo(gjeldende.opprettetAv());
        assertThat(startEskalering.opprettetDato()).isEqualToIgnoringNanos(gjeldende.opprettetDato());
        assertThat(startEskalering.opprettetBegrunnelse()).isEqualTo(gjeldende.opprettetBegrunnelse());

        ConsumerRecord<NokkelInput, OppgaveInput> brukernotifikasjonRecord = KafkaTestUtils.getSingleRecord(brukerNotifikasjonOppgaveConsumer, brukernotifikasjonOppgaveTopic, DEFAULT_WAIT_TIMEOUT);

        NokkelInput nokkelInput = brukernotifikasjonRecord.key();
        OppgaveInput oppgaveInput = brukernotifikasjonRecord.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(nokkelInput.getFodselsnummer()).isEqualTo(bruker.getFnr());
            assertions.assertThat(nokkelInput.getAppnavn()).isEqualTo(applicationName);
            assertions.assertThat(nokkelInput.getNamespace()).isEqualTo(namespace);
            assertions.assertThat(nokkelInput.getEventId()).isNotEmpty();
            assertions.assertThat(nokkelInput.getGrupperingsId()).isEqualTo(dialogDTO.getOppfolgingsperiode().toString());

            assertions.assertThat(oppgaveInput.getEksternVarsling()).isTrue();
            assertions.assertThat(oppgaveInput.getSikkerhetsnivaa()).isEqualTo(sikkerhetsNivaa);
            assertions.assertThat(oppgaveInput.getLink()).isEqualTo(eventLink);
            assertions.assertThat(oppgaveInput.getTekst()).isEqualTo(brukernotifikasjonEventTekst);

            assertions.assertThat(oppgaveInput.getEpostVarslingstittel()).isNull();
            assertions.assertThat(oppgaveInput.getEpostVarslingstekst()).isNull();
            assertions.assertThat(oppgaveInput.getSmsVarslingstekst()).isNull();
            assertions.assertAll();
        });
    }

    @Test
    void stop_eskalering_med_henvendelse() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String avsluttBegrunnelse = "Du har gjort aktiviteten vi ba om.";
        Fnr brukerFnr = Fnr.of(bruker.getFnr());

        StartEskaleringDto startEskaleringDto = new StartEskaleringDto(brukerFnr, "begrunnelse", "overskrift", "tekst");
        EskaleringsvarselDto eskaleringsvarsel = dialogTestService.startEskalering(veileder, startEskaleringDto);

        StopEskaleringDto stopEskaleringDto = new StopEskaleringDto(brukerFnr, avsluttBegrunnelse, true);
        stopEskalering(veileder, stopEskaleringDto);

        DialogDTO dialogDTO = dialogTestService.hentDialog(veileder, eskaleringsvarsel.tilhorendeDialogId());

        SoftAssertions.assertSoftly(
                assertions -> {
                    List<HenvendelseDTO> henvendelser = dialogDTO.getHenvendelser();

                    assertions.assertThat(henvendelser).hasSize(2);

                    HenvendelseDTO stopEskaleringHendvendelse = dialogDTO.getHenvendelser().get(1);
                    assertions.assertThat(stopEskaleringHendvendelse.getTekst()).isEqualTo(avsluttBegrunnelse);
                    assertions.assertThat(stopEskaleringHendvendelse.getAvsenderId()).isEqualTo(veileder.getNavIdent());
                }
        );

        ingenGjeldende(veileder, bruker);


        ConsumerRecord<NokkelInput, DoneInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, DEFAULT_WAIT_TIMEOUT);

        NokkelInput nokkelInput = brukernotifikasjonRecord.key();
        DoneInput doneInput = brukernotifikasjonRecord.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(nokkelInput.getFodselsnummer()).isEqualTo(bruker.getFnr());
            assertions.assertThat(nokkelInput.getAppnavn()).isEqualTo(applicationName);
            assertions.assertThat(nokkelInput.getNamespace()).isEqualTo(namespace);
            assertions.assertThat(nokkelInput.getEventId()).isNotEmpty();
            assertions.assertThat(nokkelInput.getGrupperingsId()).isEqualTo(dialogDTO.getOppfolgingsperiode().toString());

            assertions.assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(doneInput.getTidspunkt()), ZoneOffset.UTC)).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(10, ChronoUnit.SECONDS));
            assertions.assertAll();
        });
    }

    @Test
    void stop_eskalering_uten_henvendelse() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String avsluttBegrunnelse = "Fordi ...";
        Fnr brukerFnr = Fnr.of(bruker.getFnr());

        StartEskaleringDto startEskaleringDto = new StartEskaleringDto(brukerFnr, "begrunnelse", "overskrift", "tekst");
        EskaleringsvarselDto eskaleringsvarsel = dialogTestService.startEskalering(veileder, startEskaleringDto);

        StopEskaleringDto stopEskaleringDto = new StopEskaleringDto(brukerFnr, avsluttBegrunnelse, false);
        stopEskalering(veileder, stopEskaleringDto);

        DialogDTO dialogDTO = dialogTestService.hentDialog(veileder, eskaleringsvarsel.tilhorendeDialogId());

        SoftAssertions.assertSoftly(
                assertions -> {
                    List<HenvendelseDTO> hendvendelser = dialogDTO.getHenvendelser();
                    assertions.assertThat(hendvendelser).hasSize(1);
                }
        );

        ingenGjeldende(veileder, bruker);


        ConsumerRecord<NokkelInput, DoneInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, DEFAULT_WAIT_TIMEOUT);

        NokkelInput nokkelInput = brukernotifikasjonRecord.key();
        DoneInput doneInput = brukernotifikasjonRecord.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(nokkelInput.getFodselsnummer()).isEqualTo(bruker.getFnr());
            assertions.assertThat(nokkelInput.getAppnavn()).isEqualTo(applicationName);
            assertions.assertThat(nokkelInput.getNamespace()).isEqualTo(namespace);
            assertions.assertThat(nokkelInput.getEventId()).isNotEmpty();
            assertions.assertThat(nokkelInput.getGrupperingsId()).isEqualTo(dialogDTO.getOppfolgingsperiode().toString());

            assertions.assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(doneInput.getTidspunkt()), ZoneOffset.UTC)).isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(10, ChronoUnit.SECONDS));
            assertions.assertAll();
        });
    }

    @Test
    void bruker_kan_ikke_varsles() {
        MockBruker bruker = MockNavService.createHappyBruker();
        BrukerOptions reservertKrr = bruker.getBrukerOptions().toBuilder().erReservertKrr(true).build();
        MockNavService.updateBruker(bruker, reservertKrr);

        MockVeileder veileder = MockNavService.createVeileder(bruker);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        Response response = tryStartEskalering(veileder, startEskaleringDto);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        ingenGjeldende(veileder, bruker);
    }

    @Test
    void bruker_ikke_under_oppfolging() {
        MockBruker bruker = MockNavService.createHappyBruker();
        BrukerOptions reservertKrr = bruker.getBrukerOptions().toBuilder().underOppfolging(false).build();
        MockNavService.updateBruker(bruker, reservertKrr);

        MockVeileder veileder = MockNavService.createVeileder(bruker);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        Response response = tryStartEskalering(veileder, startEskaleringDto);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        ingenGjeldende(veileder, bruker);
    }

    @Test
    void bruker_har_allerede_aktiv_eskalering() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        dialogTestService.startEskalering(veileder, startEskaleringDto);
        Response response = tryStartEskalering(veileder, startEskaleringDto);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void test_historikk() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        dialogTestService.startEskalering(veileder, startEskaleringDto);
        StopEskaleringDto stopEskaleringDto =
                new StopEskaleringDto(Fnr.of(bruker.getFnr()), "avsluttbegrunnelse", false);
        stopEskalering(veileder, stopEskaleringDto);
        dialogTestService.startEskalering(veileder, startEskaleringDto);
        stopEskalering(veileder, stopEskaleringDto);
        dialogTestService.startEskalering(veileder, startEskaleringDto);
        stopEskalering(veileder, stopEskaleringDto);
        dialogTestService.startEskalering(veileder, startEskaleringDto);


        List<EskaleringsvarselDto> eskaleringsvarselDtos = hentHistorikk(veileder, bruker);
        assertThat(eskaleringsvarselDtos).hasSize(4);
        EskaleringsvarselDto eldste = eskaleringsvarselDtos.get(3);

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(eldste.tilhorendeDialogId()).isNotNull();
            assertions.assertThat(eldste.id()).isNotNull();
            assertions.assertThat(eldste.opprettetAv()).isEqualTo(veileder.getNavIdent());
            assertions.assertThat(eldste.opprettetBegrunnelse()).isEqualTo("begrunnelse");
            assertions.assertThat(eldste.avsluttetBegrunnelse()).isEqualTo("avsluttbegrunnelse");
            assertions.assertThat(eldste.avsluttetAv()).isEqualTo(veileder.getNavIdent());
            assertions.assertThat(eldste.opprettetDato()).isCloseTo(ZonedDateTime.now(), within(5, ChronoUnit.SECONDS));
            assertions.assertThat(eldste.avsluttetDato()).isCloseTo(ZonedDateTime.now(), within(5, ChronoUnit.SECONDS));

        });

    }

    @Test
    void skal_kun_prosessere_en_ved_samtidige_kall() throws Exception {
        int antallKall = 10;
        ExecutorService bakgrunnService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(antallKall);

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "Dialog overskrift", "henvendelseTekst");

        for (int i = 0; i < antallKall; i++) {
            bakgrunnService.submit(() -> {
                try {
                    dialogTestService.startEskalering(veileder, startEskaleringDto);
                } catch (Exception e) {
                    log.warn("Feil i tråd.", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        requireGjeldende(veileder, bruker);

        KafkaTestUtils.getSingleRecord(brukerNotifikasjonOppgaveConsumer, brukernotifikasjonOppgaveTopic, DEFAULT_WAIT_TIMEOUT);
        kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonOppgaveTopic, brukerNotifikasjonOppgaveConsumer);
    }

    @Test
    void hentGjeldendeSomEksternbruker() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        dialogTestService.startEskalering(veileder, startEskaleringDto);

        bruker.createRequest()
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response();
    }

    @Test
    void hentGjeldendeSomVeilederUtenFnrParam() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        dialogTestService.startEskalering(veileder, startEskaleringDto);

        veileder.createRequest()
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
                .then()
                .assertThat().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void send_done_naar_eskalering_lest_av_bruker() {

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String begrunnelse = "Fordi ...";
        String overskrift = "Dialog tittel";
        String henvendelseTekst = "Henvendelsestekst... lang tekst";

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), begrunnelse, overskrift, henvendelseTekst);
        EskaleringsvarselDto startEskalering = dialogTestService.startEskalering(veileder, startEskaleringDto);

        lesHenvendelse(bruker, startEskalering.tilhorendeDialogId());

        ConsumerRecord<NokkelInput, DoneInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, DEFAULT_WAIT_TIMEOUT);

        NokkelInput nokkel = brukernotifikasjonRecord.key();

        assertThat(bruker.getFnr()).isEqualTo(nokkel.getFodselsnummer());
    }

    @Test
    void unngaaDobleNotifikasjonerPaaEskaleringsvarsel() throws InterruptedException {

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);


        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        dialogTestService.startEskalering(veileder, startEskaleringDto);

        Thread.sleep(1000L);
        // Batchen bestiller beskjeder ved nye dialoger (etter 1000 ms)
        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();

        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        requireGjeldende(veileder, bruker);

        KafkaTestUtils.getSingleRecord(brukerNotifikasjonOppgaveConsumer, brukernotifikasjonOppgaveTopic, DEFAULT_WAIT_TIMEOUT);
        // sjekk at det ikke ble sendt beskjed på dialogmelding
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonBeskjedTopic, brukerNotifikasjonBeskjedConsumer));


    }

    private List<EskaleringsvarselDto> hentHistorikk(MockVeileder veileder, MockBruker mockBruker) {
        return veileder.createRequest()
                .param("fnr", mockBruker.getFnr())
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/historikk")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().jsonPath().getList(".", EskaleringsvarselDto.class);
    }

    private Response tryStartEskalering(MockVeileder veileder, StartEskaleringDto startEskaleringDto) {
        Response response = veileder.createRequest()
                .body(startEskaleringDto)
                .when()
                .post("/veilarbdialog/api/eskaleringsvarsel/start")
                .then()
                .extract().response();
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();
        return response;
    }

    private void stopEskalering(MockVeileder veileder, StopEskaleringDto stopEskaleringDto) {
        dialogTestService.stoppEskalering(veileder, stopEskaleringDto);
    }

    private void lesHenvendelse(MockBruker bruker, long dialogId) {
        bruker.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialogId)
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(DialogDTO.class);
        brukernotifikasjonService.sendDoneBrukernotifikasjoner();
    }

    private GjeldendeEskaleringsvarselDto requireGjeldende(MockVeileder veileder, MockBruker mockBruker) {
        Response response = veileder.createRequest()
                .param("fnr", mockBruker.getFnr())
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        return response.as(GjeldendeEskaleringsvarselDto.class);
    }


    private void ingenGjeldende(MockVeileder veileder, MockBruker mockBruker) {
        veileder.createRequest()
                .param("fnr", mockBruker.getFnr())
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
                .then()
                .assertThat().statusCode(HttpStatus.NO_CONTENT.value())
                .extract()
                .response();
    }

}
