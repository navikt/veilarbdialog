package no.nav.fo.veilarbdialog.rest;

import io.restassured.http.ContentType;
import lombok.val;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.feed.KvpService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = AFTER_TEST_METHOD
)
public class DialogRessursTest {
    final static String fnr = "12345";
    final static String aktorId = "54321";

    @LocalServerPort
    private int port;

    @MockBean
    private AuthService authService;

    @Autowired
    JdbcTemplate jdbc;

    @MockBean
    private KvpService kvpService;

    @Autowired
    private AktorOppslagClient aktorOppslagClient;

    @Autowired
    private DialogRessurs dialogRessurs;

    @Before
    public void before() {
        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn(null);
        when(aktorOppslagClient.hentAktorId(Fnr.of(fnr))).thenReturn(AktorId.of(aktorId));
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.harTilgangTilPerson(anyString())).thenReturn(true);
        when(authService.getIdent()).thenReturn(Optional.of("101"));
        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn(null);
        when(authService.getIdent()).thenReturn(Optional.of(fnr));
    }

    @Test
    public void nyHenvendelse() {

        NyHenvendelseDTO dto = new NyHenvendelseDTO()
                .setTekst("tekst");
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(dto)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", "1337")
                .then()
                .assertThat()
                .statusCode(200)
                .body("sisteTekst", is("tekst"))
                .body("henvendelser.size()", is(1))
                .body("henvendelser[0].avsender", is("BRUKER"))
                .body("henvendelser[0].tekst", is("tekst"));

    }

    public void hentDialoger() {
        dialogRessurs.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        List<DialogDTO> dialoger = dialogRessurs.hentDialoger();
        assertThat(dialoger.size()).isEqualTo(1);
    }


    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnes_oppdatererEgenskap() {
        final String aktivitetId = "123";

        dialogRessurs.nyHenvendelse(
                new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId(aktivitetId)
        );

        val opprettetDialog = dialogRessurs.hentDialoger();
        assertThat(opprettetDialog.get(0).getEgenskaper().isEmpty()).isTrue();
        assertThat(opprettetDialog.size()).isEqualTo(1);

        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId(aktivitetId)
        );

        val dialogMedParagraf8 = dialogRessurs.hentDialoger();
        assertThat(dialogMedParagraf8.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
        assertThat(dialogMedParagraf8.size()).isEqualTo(1);
    }

    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnesIkke_oppdatererEgenskap() {
        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId("123")
        );

        val hentedeDialoger = dialogRessurs.hentDialoger();
        assertThat(hentedeDialoger.size()).isEqualTo(1);
        assertThat(hentedeDialoger.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
    }
}
