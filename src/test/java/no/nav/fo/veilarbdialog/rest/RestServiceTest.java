package no.nav.fo.veilarbdialog.rest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.StatusDAO;
import no.nav.fo.veilarbdialog.db.dao.VarselDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.feed.KvpService;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.fo.veilarbdialog.service.DialogDataService;

import no.nav.fo.veilarbdialog.service.DialogStatusService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class RestServiceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
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

        mockAuthOK();

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

    private void mockAuthOK(){
        Fnr fnr = Fnr.of("1234");
        AktorId aktorId = AktorId.of("4321");
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.getIdent()).thenReturn(Optional.of(fnr.get()));
        when(authService.harTilgangTilPerson(aktorId.get())).thenReturn(true);
        when(kontorsperreFilter.tilgangTilEnhet(ArgumentMatchers.any(DialogData.class))).thenReturn(true);
        when(kontorsperreFilter.tilgangTilEnhet(ArgumentMatchers.any(HenvendelseData.class))).thenReturn(true);

        when(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId);
        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn(null);
        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn(null);

    }
}
