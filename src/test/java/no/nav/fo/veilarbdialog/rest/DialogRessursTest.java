package no.nav.fo.veilarbdialog.rest;

import io.restassured.http.ContentType;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class DialogRessursTest {

    private static final String AKTORID = "123";
    private static final String FNR = "4321";

    @LocalServerPort
    private int port;

    @MockBean
    private DataVarehusDAO dataVarehusDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private AuthService authService;

    @Before
    public void before() {
        when(authService.activeUserHasReadAccessToPerson(anyString())).thenReturn(true);
    }

    @Test
    public void sistOppdatert() {

        Date timestamp = new Date();
        when(dataVarehusDAO.hentSisteEndringSomIkkeErDine(anyString(), anyString())).thenReturn(timestamp);
        when(authService.getIdent()).thenReturn(Optional.of("123"));

        given()
                .port(port)
                .param("aktorId", "123")
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .body("sistOppdatert", equalTo(timestamp.getTime()));

    }

    /*@Test
    public void opprettOgHentDialoger() throws Exception {
        dialogRessurs.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        val hentAktiviteterResponse = dialogRessurs.hentDialoger();
        assertThat(hentAktiviteterResponse, hasSize(1));

        dialogRessurs.markerSomLest(hentAktiviteterResponse.get(0).id);
    }


    @Test
    public void forhandsorienteringPaEksisterendeDialogPaAktivitetSkalFaEgenskapenParagraf8() {
        final String aktivitetId = "123";

        dialog.nyHenvendelse(
                new NyHenvendelseDTO()
                        .setTekst("forhandsorienteringPaEksisterendeDialogPaAktivitetSkalFaEgenskapenParagraf8")
                        .setAktivitetId(aktivitetId)
        );

        val opprettetDialog = dialog.hentDialoger();
        assertThat(opprettetDialog.get(0).getEgenskaper().isEmpty(), is(true));
        assertThat(opprettetDialog.size(), is(1));

        dialog.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("paragraf8")
                        .setAktivitetId(aktivitetId)
        );

        val dialogMedParagraf8 = dialog.hentDialoger();
        assertThat(dialogMedParagraf8.get(0).getEgenskaper().contains(Egenskap.PARAGRAF8), is(true));
        assertThat(dialogMedParagraf8.size(), is(1));
    }

    @Test
    public void skalHaParagraf8Egenskap() {
        dialog.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("skalHaParagraf8Egenskap")
                        .setAktivitetId("123")
        );

        val hentedeDialoger = dialog.hentDialoger();
        assertThat(hentedeDialoger, hasSize(1));
        assertThat(hentedeDialoger.get(0).getEgenskaper().contains(Egenskap.PARAGRAF8), is(true));
    }*/
}
