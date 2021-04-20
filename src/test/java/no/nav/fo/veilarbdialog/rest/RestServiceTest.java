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
import no.nav.fo.veilarbdialog.feed.KvpService;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
public class RestServiceTest {
    final static Fnr fnr = Fnr.of("1234");
    final static AktorId aktorId = AktorId.of("4321");
    final static String veilederIdent = "V123456";


    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    KontorsperreFilter kontorsperreFilter;

    @MockBean
    AuthService authService;

    @MockBean
    KvpService kvpService;

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
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void after() {
        jdbc.update("delete from HENVENDELSE");
        jdbc.update("delete from DIALOG");
        jdbc.update("delete from DIALOG_AKTOR");
        jdbc.update("delete from EVENT");


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

        mockHappyPathBruker();
        mockKVPOnUser(null);

        DialogDTO resultatDialog = given()
                .contentType(ContentType.JSON)
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
        HenvendelseDTO resultatHenvendelse = resultatDialog.henvendelser.get(0);

        assertThat(resultatDialog).isEqualToIgnoringGivenFields(expected,"opprettetDato", "sisteDato", "henvendelser", "id");
        assertThat(resultatHenvendelse).isEqualToIgnoringGivenFields(henvendelseExpected, "sendt", "id", "dialogId");
        assertThat(resultatDialog.id).isEqualTo(resultatHenvendelse.dialogId);

    }

    @Test
    public void nyHenvendelse_dialogFinnesIkke_veileder() {
        String tekst = "tekst", overskrift = "overskrift";
        final NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift);

        final DialogDTO expected = new DialogDTO()
                .setOverskrift(overskrift)
                .setSisteTekst(tekst);

        final HenvendelseDTO henvendelseExpected = new HenvendelseDTO()
                .setTekst(tekst)
                .setAvsender(Avsender.VEILEDER);

        mockHappyPathVeileder();
        mockKVPOnUser(null);

        DialogDTO resultatDialog = given()
                .contentType(ContentType.JSON)
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", aktorId.get())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
        HenvendelseDTO resultatHenvendelse = resultatDialog.henvendelser.get(0);

        assertThat(resultatDialog).isEqualToIgnoringGivenFields(expected,"opprettetDato", "sisteDato", "henvendelser", "id");
        assertThat(resultatHenvendelse).isEqualToIgnoringGivenFields(henvendelseExpected, "sendt", "id", "dialogId");
        assertThat(resultatDialog.id).isEqualTo(resultatHenvendelse.dialogId);

    }

    @Test
    public void sistOppdatert_brukerInnlogget_kunBrukerHarLest_returnererNull() {
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, CURRENT_TIMESTAMP)", aktorId.get(), aktorId.get());

        mockHappyPathBruker();
        fetchSistOppdatert()
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(null));

    }

    @Test
    public void sistOppdatert_veilederInnlogget_kunVeilederHarLest_returnererNull() {

        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, CURRENT_TIMESTAMP)", aktorId.get(), veilederIdent);

        mockHappyPathVeileder();
        fetchSistOppdatert()
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

        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", aktorId.get(), aktorId.get(), brukerLest);
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (1, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", aktorId.get(), veilederIdent, veilederLest);

        mockHappyPathBruker();

        fetchSistOppdatert()
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

        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", aktorId.get(), aktorId.get(), brukerLest);
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (1, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", aktorId.get(), veilederIdent, veilederLest);

        mockHappyPathBruker();

        fetchSistOppdatert()
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(veilederLest.getTime()));

    }

    private Response fetchSistOppdatert() {
        return given()
                .param("aktorId", aktorId.get())
                .get("/veilarbdialog/api/dialog/sistOppdatert");
    }

    private void mockHappyPathVeileder(){
        when(authService.erEksternBruker()).thenReturn(false);
        when(authService.getIdent()).thenReturn(Optional.of(veilederIdent));

        mockAuthOK();
    }

    private void mockHappyPathBruker(){
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.getIdent()).thenReturn(Optional.of(fnr.get()));
        mockAuthOK();
    }

    private void mockAuthOK() {

        when(authService.harTilgangTilPerson(aktorId.get())).thenReturn(true);
        when(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId);

    }

    private void mockKVPOnUser(String unit) {

        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn(unit);
        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn(unit);
    }
}
