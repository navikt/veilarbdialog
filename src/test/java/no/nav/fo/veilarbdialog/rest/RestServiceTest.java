package no.nav.fo.veilarbdialog.rest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.Avsender;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("local")
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = AFTER_TEST_METHOD
)
public class RestServiceTest {

    MockVeileder veileder;
    MockBruker bruker;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    KontorsperreFilter kontorsperreFilter;

    @Autowired
    RestMapper restMapper;

    @Autowired
    AktorOppslagClient aktorOppslagClient;

    @Autowired
    DialogDataService dialogDataService;

    @Autowired
    DialogRessurs dialogRessurs;


    @Before
    public void before() {
        RestAssured.port = port;
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
    }

    @Test
    public void nyHenvendelse_dialogFinnesIkke_bruker() {
        String tekst = "tekst", overskrift = "overskrift";
        final NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift);

        final DialogDTO expected = new DialogDTO()
                .setOverskrift(overskrift)
                .setSisteTekst(tekst)
                .setLest(true)
                .setFerdigBehandlet(true);

        final HenvendelseDTO henvendelseExpected = new HenvendelseDTO()
                .setTekst(tekst)
                .setLest(true)
                .setAvsender(Avsender.BRUKER);


        DialogDTO resultatDialog = bruker.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
        HenvendelseDTO resultatHenvendelse = resultatDialog.getHenvendelser().get(0);

        assertThat(resultatDialog).isEqualToIgnoringGivenFields(expected,"opprettetDato", "sisteDato", "henvendelser", "id");
        assertThat(resultatHenvendelse).isEqualToIgnoringGivenFields(henvendelseExpected, "sendt", "id", "dialogId");
        assertThat(resultatDialog.getId()).isEqualTo(resultatHenvendelse.getDialogId());

    }

    @Test
    public void nyHenvendelse_dialogFinnesIkke_veileder() {
        String tekst = "tekst", overskrift = "overskrift";
        final NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift);

        final DialogDTO expected = new DialogDTO()
                .setOverskrift(overskrift)
                .setSisteTekst(tekst)
                .setLest(true)
                .setFerdigBehandlet(true);

        final HenvendelseDTO henvendelseExpected = new HenvendelseDTO()
                .setTekst(tekst)
                .setAvsender(Avsender.VEILEDER)
                .setAvsenderId(veileder.getNavIdent())
                .setLest(true);

        DialogDTO resultatDialog = veileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
        HenvendelseDTO resultatHenvendelse = resultatDialog.getHenvendelser().get(0);

        assertThat(resultatDialog).isEqualToIgnoringGivenFields(expected,"opprettetDato", "sisteDato", "henvendelser", "id");
        assertThat(resultatHenvendelse).isEqualToIgnoringGivenFields(henvendelseExpected, "sendt", "id", "dialogId");
        assertThat(resultatDialog.getId()).isEqualTo(resultatHenvendelse.getDialogId());
    }

    @Test
    public void sistOppdatert_brukerInnlogget_kunBrukerHarLest_returnererNull() {
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, CURRENT_TIMESTAMP)", bruker.getAktorId(), bruker.getAktorId());

        bruker.createRequest()
                .param("aktorId", bruker.getAktorId())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(null));
    }

    @Test
    public void sistOppdatert_veilederInnlogget_kunVeilederHarLest_returnererNull() {
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, CURRENT_TIMESTAMP)", bruker.getAktorId(), veileder.getNavIdent());

        veileder.createRequest()
                .param("aktorId", bruker.getAktorId())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(null));
    }

    @Test
    public void sistOppdatert_brukerInnlogget_veilederEndretSist_returnererTidspunkt() {
        Timestamp brukerLest = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
        Timestamp veilederLest = Timestamp.valueOf(LocalDateTime.now());

        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), bruker.getAktorId(), brukerLest);
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (1, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), veileder.getNavIdent(), veilederLest);

        bruker.createRequest()
                .param("aktorId", bruker.getAktorId())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(veilederLest.getTime()));
    }

    @Test
    public void sistOppdatert_veilederInnlogget_brukerEndretSist_returnererTidspunkt() {
        Timestamp veilederLest = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
        Timestamp brukerLest = Timestamp.valueOf(LocalDateTime.now());

        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), veileder.getNavIdent(), veilederLest);
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (1, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), bruker.getAktorId(), brukerLest);

        veileder.createRequest()
                .param("aktorId", bruker.getAktorId())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(brukerLest.getTime()));
    }
}
