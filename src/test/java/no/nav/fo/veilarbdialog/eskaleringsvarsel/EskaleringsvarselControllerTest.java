package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.GjeldendeEskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.util.DialogTestService;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class EskaleringsvarselControllerTest {

    @LocalServerPort
    protected int port;

    @Value("${application.topic.ut.brukernotifikasjon.oppgave}")
    private String brukernotifikasjonUtTopic;

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

    Consumer<NokkelInput, OppgaveInput> brukerNotifikasjonOppgaveConsumer;

    Consumer<NokkelInput, DoneInput> brukerNotifikasjonDoneConsumer;

    @Before
    public void setup() {
        RestAssured.port = port;
        brukerNotifikasjonOppgaveConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonUtTopic);
        brukerNotifikasjonDoneConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonDoneTopic);
    }

    @Test
    public void start_eskalering_happy_case() {

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String begrunnelse = "Fordi ...";
        String overskrift = "Dialog tittel";
        String henvendelseTekst = "Henvendelsestekst... lang tekst";

        // TODO fix disse. Epostvarsel skal sannsynligvis ikke inneholde de samme tekstene som dialogen.
        // TODO Hvis tekstene inneholder mulig sensitiv info, må påloggingsnivå settes til 4.

        // Tekst som brukes i eventet på DittNav. Påkrevd, ingen default
        String brukernotifikasjonEventTekst = henvendelseTekst;
        // Påloggingsnivå for å lese eventet på DittNav. Dersom eventteksten er sensitiv, må denne være 4.
        int sikkerhetsNivaa = 3;
        // Lenke som blir aktivert når bruker klikker på eventet
        String eventLink;
        // Hvis null, default "Hei! Du har fått en ny beskjed på Ditt NAV. Logg inn og se hva beskjeden gjelder. Vennlig hilsen NAV"
        String brukernotifikasjonSmsVarslingTekst = null;
        // Hvis null, default "Beskjed fra NAV"
        String brukernotifikasjonEpostVarslingTittel = overskrift;
        // Hvis null, default "<!DOCTYPE html><html><head><title>Melding</title></head><body><p>Hei!</p><p>Du har fått en ny beskjed på Ditt NAV. Logg inn og se hva beskjeden gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>"
        String brukernotifikasjonEpostVarslingTekst = henvendelseTekst;


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
        assertThat(startEskalering.opprettetDato()).isEqualTo(gjeldende.opprettetDato());
        assertThat(startEskalering.opprettetBegrunnelse()).isEqualTo(gjeldende.opprettetBegrunnelse());

        ConsumerRecord<NokkelInput, OppgaveInput> brukernotifikasjonRecord = KafkaTestUtils.getSingleRecord(brukerNotifikasjonOppgaveConsumer, brukernotifikasjonUtTopic, 5000L);

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
    public void stop_eskalering_happy_case() {
        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String avsluttBegrunnelse = "Fordi ...";
        Fnr brukerFnr = Fnr.of(bruker.getFnr());

        StartEskaleringDto startEskaleringDto = new StartEskaleringDto(brukerFnr, "begrunnelse", "overskrift", "tekst");
        EskaleringsvarselDto eskaleringsvarsel = startEskalering(veileder, startEskaleringDto);

        StopEskaleringDto stopEskaleringDto = new StopEskaleringDto(brukerFnr, avsluttBegrunnelse);
        stopEskalering(veileder, stopEskaleringDto);

        DialogDTO dialogDTO = dialogTestService.hentDialog(port, veileder, eskaleringsvarsel.tilhorendeDialogId());

        SoftAssertions.assertSoftly(
                assertions -> {
                    List<HenvendelseDTO> hendvendelser = dialogDTO.getHenvendelser();

                    assertions.assertThat(hendvendelser).hasSize(2);

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

            assertions.assertThat(doneInput.getTidspunkt()).isCloseTo(ZonedDateTime.now().toInstant().toEpochMilli(), Offset.offset(10 * 1000L));
            assertions.assertAll();
        });
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
        return eskaleringsvarselDto;
    }

    private void stopEskalering(MockVeileder veileder, StopEskaleringDto stopEskaleringDto) {
        veileder.createRequest()
                .body(stopEskaleringDto)
                .when()
                .patch("/veilarbdialog/api/eskaleringsvarsel/stop")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();
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