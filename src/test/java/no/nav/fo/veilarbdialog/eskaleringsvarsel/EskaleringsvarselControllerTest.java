package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
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
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class EskaleringsvarselControllerTest {

    @LocalServerPort
    protected int port;

    @Value("${application.topic.ut.brukernotifikasjon}")
    private String brukernotifikasjonUtTopic;



    @Autowired
    DialogTestService dialogTestService;

    @Autowired
    KafkaTestService kafkaTestService;


    Consumer<NokkelInput, OppgaveInput> brukerNotifikasjonConsumer;


    @Before
    public void setup() {
        RestAssured.port = port;
        brukerNotifikasjonConsumer = kafkaTestService.createAvroAvroConsumer(brukernotifikasjonUtTopic);
    }

    @Test
    public void happyCase() {

        MockBruker bruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(bruker);
        String begrunnelse = "Fordi ...";
        String overskrift = "Du har fått en viktig beskjed fra Nav";
        String tekst = "Bla bla bla, du må ...";
        StartEskaleringDto startEskaleringDto =
                new StartEskaleringDto(bruker.getFnr(), begrunnelse, overskrift, tekst);
        EskaleringsvarselDto startEskalering = startEskalering(veileder, startEskaleringDto);

        EskaleringsvarselDto gjeldende = hentGjeldende(veileder, bruker);

        assertThat(startEskalering).isEqualTo(gjeldende);


        DialogDTO dialogDTO = dialogTestService.hentDialog(port, veileder, gjeldende.tilhorendeDialogId());
        SoftAssertions.assertSoftly(
                assertions -> {
                    assertions.assertThat(dialogDTO.isFerdigBehandlet()).isTrue();
                    assertions.assertThat(dialogDTO.isVenterPaSvar()).isTrue();
                    HenvendelseDTO henvendelseDTO = dialogDTO.getHenvendelser().get(0);
                    assertions.assertThat(henvendelseDTO.getTekst()).isEqualTo(tekst);
                    assertions.assertThat(henvendelseDTO.getAvsenderId()).isEqualTo(veileder.getNavIdent());
                }
        );


        ConsumerRecord<NokkelInput, OppgaveInput> brukernotifikasjonRecord = KafkaTestUtils.getSingleRecord(brukerNotifikasjonConsumer, brukernotifikasjonUtTopic, 5000L);

        NokkelInput nokkelInput = brukernotifikasjonRecord.key();
        OppgaveInput oppgaveInput = brukernotifikasjonRecord.value();

        SoftAssertions.assertSoftly(assertions -> {
           assertions.assertThat(nokkelInput.getFodselsnummer()).isEqualTo(bruker.getFnr());
           assertions.assertThat(nokkelInput.getAppnavn()).isEqualTo("veilarbdialog"); // TODO sjekk
           assertions.assertThat(nokkelInput.getEventId()).isNotEmpty();
           assertions.assertThat(nokkelInput.getGrupperingsId()).isNotEmpty(); // TODO sjekk

            assertions.assertThat(oppgaveInput.getEksternVarsling()).isTrue();
            assertions.assertThat(oppgaveInput.getLink()).isEqualTo("https://www.nav.no/arbeid/dialog/something");
            assertions.assertThat(oppgaveInput.getTekst()).isEqualTo(overskrift);
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

    private EskaleringsvarselDto hentGjeldende(MockVeileder veileder, MockBruker mockBruker) {
        return veileder.createRequest()
                .param("aktorId", mockBruker.getAktorId())
                .when()
                .get("/veilarbdialog/api/eskaleringsvarsel/gjeldende")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(EskaleringsvarselDto.class);
    }

}