package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.types.identer.Fnr;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Slf4j
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = BEFORE_TEST_METHOD
)
public class EskaleringsvarselControllerTest {

    @LocalServerPort
    protected int port;

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

    @Autowired
    LockProvider lockProvider;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Consumer<NokkelInput, OppgaveInput> brukerNotifikasjonOppgaveConsumer;

    Consumer<NokkelInput, BeskjedInput> brukerNotifikasjonBeskjedConsumer;

    Consumer<NokkelInput, DoneInput> brukerNotifikasjonDoneConsumer;

    @Before
    public void setup() {
        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();
        RestAssured.port = port;
        brukerNotifikasjonOppgaveConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonOppgaveTopic);
        brukerNotifikasjonBeskjedConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonBeskjedTopic);
        brukerNotifikasjonDoneConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonDoneTopic);
    }


    @Test
    public void start_eskalering_happy_case() {

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
        // Hvis null, default "Hei! Du har fått en ny beskjed på Ditt NAV. Logg inn og se hva beskjeden gjelder. Vennlig hilsen NAV"
        String brukernotifikasjonSmsVarslingTekst = "Hei! Du har fått en ny viktig oppgave på Ditt NAV. Logg inn og se hva oppgaven gjelder. Vennlig hilsen NAV";
        // Hvis null, default "Beskjed fra NAV"
        String brukernotifikasjonEpostVarslingTittel = "Viktig oppgave";
        // Hvis null, default "<!DOCTYPE html><html><head><title>Melding</title></head><body><p>Hei!</p><p>Du har fått en ny beskjed på Ditt NAV. Logg inn og se hva beskjeden gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>"
        String brukernotifikasjonEpostVarslingTekst = "Du har fått en ny viktig oppgave fra NAV. Logg inn og se hva oppgaven gjelder. Vennlig hilsen NAV";


        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), begrunnelse, overskrift, henvendelseTekst);
        EskaleringsvarselDto startEskalering = startEskalering(veileder, startEskaleringDto);


        DialogDTO dialogDTO = dialogTestService.hentDialog(port, veileder, startEskalering.tilhorendeDialogId());

        eventLink = dialogUrl + "/" + dialogDTO.getId();
        SoftAssertions.assertSoftly(
                assertions -> {
                    assertions.assertThat(dialogDTO.isFerdigBehandlet()).isTrue();
                    assertions.assertThat(dialogDTO.isVenterPaSvar()).isTrue();
                    HenvendelseDTO henvendelseDTO = dialogDTO.getHenvendelser().get(0);
                    assertions.assertThat(henvendelseDTO.getTekst()).isEqualTo(henvendelseTekst);
                    assertions.assertThat(henvendelseDTO.getAvsenderId()).isEqualTo(veileder.getNavIdent());
                }
        );

        GjeldendeEskaleringsvarselDto gjeldende = requireGjeldende(veileder, bruker);

        assertThat(startEskalering.id()).isEqualTo(gjeldende.id());
        assertThat(startEskalering.tilhorendeDialogId()).isEqualTo(gjeldende.tilhorendeDialogId());
        assertThat(startEskalering.opprettetAv()).isEqualTo(gjeldende.opprettetAv());
        assertThat(startEskalering.opprettetDato()).isEqualToIgnoringNanos(gjeldende.opprettetDato());
        assertThat(startEskalering.opprettetBegrunnelse()).isEqualTo(gjeldende.opprettetBegrunnelse());

        ConsumerRecord<NokkelInput, OppgaveInput> brukernotifikasjonRecord = KafkaTestUtils.getSingleRecord(brukerNotifikasjonOppgaveConsumer, brukernotifikasjonOppgaveTopic, 5000L);

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

            assertions.assertThat(oppgaveInput.getEpostVarslingstittel()).isEqualTo(brukernotifikasjonEpostVarslingTittel);
            assertions.assertThat(oppgaveInput.getEpostVarslingstekst()).isEqualTo(brukernotifikasjonEpostVarslingTekst);
            assertions.assertThat(oppgaveInput.getSmsVarslingstekst()).isEqualTo(brukernotifikasjonSmsVarslingTekst);
            assertions.assertAll();
        });
    }

    @Test
    public void stop_eskalering_med_henvendelse() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String avsluttBegrunnelse = "Du har gjort aktiviteten vi ba om.";
        Fnr brukerFnr = Fnr.of(bruker.getFnr());

        StartEskaleringDto startEskaleringDto = new StartEskaleringDto(brukerFnr, "begrunnelse", "overskrift", "tekst");
        EskaleringsvarselDto eskaleringsvarsel = startEskalering(veileder, startEskaleringDto);

        StopEskaleringDto stopEskaleringDto = new StopEskaleringDto(brukerFnr, avsluttBegrunnelse, true);
        stopEskalering(veileder, stopEskaleringDto);

        DialogDTO dialogDTO = dialogTestService.hentDialog(port, veileder, eskaleringsvarsel.tilhorendeDialogId());

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
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, 5000L);

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
    public void stop_eskalering_uten_henvendelse() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String avsluttBegrunnelse = "Fordi ...";
        Fnr brukerFnr = Fnr.of(bruker.getFnr());

        StartEskaleringDto startEskaleringDto = new StartEskaleringDto(brukerFnr, "begrunnelse", "overskrift", "tekst");
        EskaleringsvarselDto eskaleringsvarsel = startEskalering(veileder, startEskaleringDto);

        StopEskaleringDto stopEskaleringDto = new StopEskaleringDto(brukerFnr, avsluttBegrunnelse, false);
        stopEskalering(veileder, stopEskaleringDto);

        DialogDTO dialogDTO = dialogTestService.hentDialog(port, veileder, eskaleringsvarsel.tilhorendeDialogId());

        SoftAssertions.assertSoftly(
                assertions -> {
                    List<HenvendelseDTO> hendvendelser = dialogDTO.getHenvendelser();
                    assertions.assertThat(hendvendelser).hasSize(1);
                }
        );

        ingenGjeldende(veileder, bruker);


        ConsumerRecord<NokkelInput, DoneInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, 5000L);

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
    public void bruker_kan_ikke_varsles() {
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
    public void bruker_ikke_under_oppfolging() {
        MockBruker bruker = MockNavService.createHappyBruker();
        BrukerOptions reservertKrr = bruker.getBrukerOptions().toBuilder().underOppfolging(false).build() ;
        MockNavService.updateBruker(bruker, reservertKrr);

        MockVeileder veileder = MockNavService.createVeileder(bruker);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        Response response = tryStartEskalering(veileder, startEskaleringDto);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        ingenGjeldende(veileder, bruker);
    }

    @Test
    public void bruker_har_allerede_aktiv_eskalering() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        startEskalering(veileder, startEskaleringDto);
        Response response = tryStartEskalering(veileder, startEskaleringDto);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    public void test_historikk() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        startEskalering(veileder, startEskaleringDto);
        StopEskaleringDto stopEskaleringDto =
                new StopEskaleringDto(Fnr.of(bruker.getFnr()), "avsluttbegrunnelse", false);
        stopEskalering(veileder, stopEskaleringDto);
        startEskalering(veileder, startEskaleringDto);
        stopEskalering(veileder, stopEskaleringDto);
        startEskalering(veileder, startEskaleringDto);
        stopEskalering(veileder, stopEskaleringDto);
        startEskalering(veileder, startEskaleringDto);


        List<EskaleringsvarselDto> eskaleringsvarselDtos = hentHistorikk(veileder, bruker);
        assertThat(eskaleringsvarselDtos).hasSize(4);
        EskaleringsvarselDto eldste = eskaleringsvarselDtos.get(3);

        SoftAssertions.assertSoftly( assertions -> {
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
    public void skal_kun_prosessere_en_ved_samtidige_kall() throws Exception {
        int antallKall = 10;
        ExecutorService bakgrunnService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(antallKall);

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "Dialog overskrift", "henvendelseTekst");
        final EskaleringsvarselDto[] startEskalering = new EskaleringsvarselDto[1];


        for (int i = 0; i < antallKall; i++) {
            bakgrunnService.submit(() -> {
                    try {
                        startEskalering[0] = startEskalering(veileder, startEskaleringDto);
                    } catch (Exception e) {
                        log.warn("Feil i tråd.", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        latch.await();

        EskaleringsvarselDto eskaleringsvarselDto = startEskalering[0];

        requireGjeldende(veileder, bruker);

        ConsumerRecord<NokkelInput, OppgaveInput> brukernotifikasjonRecord = KafkaTestUtils.getSingleRecord(brukerNotifikasjonOppgaveConsumer, brukernotifikasjonOppgaveTopic, 5000L);
        kafkaTestService.harKonsumertAlleMeldinger(brukernotifikasjonOppgaveTopic, brukerNotifikasjonOppgaveConsumer);
    }

    @Test
    public void hentGjeldendeSomEksternbruker() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        startEskalering(veileder, startEskaleringDto);

        Response response = bruker.createRequest()
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response();
    }

    @Test
    public void hentGjeldendeSomVeilederUtenFnrParam() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        startEskalering(veileder, startEskaleringDto);

        veileder.createRequest()
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
                .then()
                .assertThat().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void send_done_naar_eskalering_lest_av_bruker() {

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String begrunnelse = "Fordi ...";
        String overskrift = "Dialog tittel";
        String henvendelseTekst = "Henvendelsestekst... lang tekst";

        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), begrunnelse, overskrift, henvendelseTekst);
        EskaleringsvarselDto startEskalering = startEskalering(veileder, startEskaleringDto);

        lesHenvendelse(bruker, startEskalering.tilhorendeDialogId());

        ConsumerRecord<NokkelInput, DoneInput> brukernotifikasjonRecord =
                KafkaTestUtils.getSingleRecord(brukerNotifikasjonDoneConsumer, brukernotifikasjonDoneTopic, 5000L);

        NokkelInput nokkel = brukernotifikasjonRecord.key();

        assertThat(bruker.getFnr()).isEqualTo(nokkel.getFodselsnummer());
    }

    @Test
    public void unngaaDobleNotifikasjonerPaaEskaleringsvarsel() {

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);



        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(Fnr.of(bruker.getFnr()), "begrunnelse", "overskrift", "henvendelseTekst");
        EskaleringsvarselDto startEskalering = startEskalering(veileder, startEskaleringDto);

        // Setter sendt til å være 30 minutter tidligere pga. grace period
        Date nySendt = Date.from(Instant.now().minus(30, ChronoUnit.MINUTES));
        jdbcTemplate.update("""
                UPDATE HENVENDELSE 
                SET SENDT = ?
                WHERE DIALOG_ID = ?
        """,
                nySendt,
                startEskalering.tilhorendeDialogId());
        // Batchen bestiller beskjeder ved nye dialoger (etter 30 min)
        scheduleRessurs.sendBrukernotifikasjonerForUlesteDialoger();

        brukernotifikasjonService.sendPendingBrukernotifikasjoner();

        requireGjeldende(veileder, bruker);

        KafkaTestUtils.getSingleRecord(brukerNotifikasjonOppgaveConsumer, brukernotifikasjonOppgaveTopic, 5000L);
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

    private EskaleringsvarselDto startEskalering(MockVeileder veileder, StartEskaleringDto startEskaleringDto) {
        Response response = veileder.createRequest()
                .body(startEskaleringDto)
                .when()
                .post("/veilarbdialog/api/eskaleringsvarsel/start")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();
        EskaleringsvarselDto eskaleringsvarselDto = response.as(EskaleringsvarselDto.class);
        assertNotNull(eskaleringsvarselDto);
        // Scheduled task
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();
        return eskaleringsvarselDto;
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
        veileder.createRequest()
                .body(stopEskaleringDto)
                .when()
                .patch("/veilarbdialog/api/eskaleringsvarsel/stop")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();
        brukernotifikasjonService.sendDoneBrukernotifikasjoner();
    }

    private DialogDTO lesHenvendelse(MockBruker bruker, long dialogId) {
        DialogDTO dialog = bruker.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialogId)
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(DialogDTO.class);
        brukernotifikasjonService.sendDoneBrukernotifikasjoner();
        return dialog;
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