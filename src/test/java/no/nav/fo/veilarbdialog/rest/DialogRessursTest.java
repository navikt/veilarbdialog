package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.auth.AuthService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class DialogRessursTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private AuthService authService;

    @Before
    public void before() {
        when(authService.harTilgangTilPerson(anyString())).thenReturn(true);
        when(authService.getIdent()).thenReturn(Optional.of("101"));
    }

    @After
    public void after() {
        jdbc.update("delete from DIALOG where DIALOG_ID = 0");
        jdbc.update("delete from EVENT where EVENT_ID = 0");
    }

    @Test
    public void sistOppdatert() {

        jdbc.update("insert into DIALOG (DIALOG_ID, OPPDATERT) values (0, current_timestamp(6))");
        jdbc.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', '1337', '1337', CURRENT_TIMESTAMP)");
        Timestamp timestamp = jdbc.queryForObject("select TIDSPUNKT from EVENT where EVENT_ID = 0", Timestamp.class);
        assertThat(timestamp).isNotNull();

        given()
                .port(port)
                .param("aktorId", "1337")
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
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
