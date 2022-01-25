package no.nav.fo.veilarbdialog.rest;

import io.restassured.RestAssured;
import lombok.val;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.apache.commons.compress.utils.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = AFTER_TEST_METHOD
)
public class DialogRessursTest {

    @LocalServerPort
    private int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DialogRessurs dialogRessurs;

    private MockBruker bruker;
    private MockVeileder veileder;

    @Before
    public void setup() {
        RestAssured.port = port;
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
    }

    @Test
    public void hentDialoger_bruker() {
        veileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200);

        List<DialogDTO> dialoger = bruker.createRequest()
                .get("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

        assertThat(dialoger).hasSize(1);
    }

    @Test
    public void nyHenvendelse_fraBruker_venterPaaNav() {
        DialogDTO dialog = bruker.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);


        //Bruker skal ikke vite om nav har ferdig behandlet dialogen
        assertThat(dialog.isVenterPaSvar()).isFalse();
        assertThat(dialog.isFerdigBehandlet()).isTrue();

        DialogDTO veiledersDialog = veileder.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", dialog.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(veiledersDialog.isVenterPaSvar()).isFalse();
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();
    }

    @Test
    public void nyHenvendelse_fraVeileder_venterIkkePaaNoen() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        DialogDTO dialog = veileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(dialog.isVenterPaSvar()).isFalse();
        assertThat(dialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    public void nyHenvendelse_veilederSvarerPaaBrukersHenvendelse_venterIkkePaaNav() {

        DialogDTO brukersDialog = bruker.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        NyHenvendelseDTO veiledersHenvendelse = new NyHenvendelseDTO().setTekst("tekst").setDialogId(brukersDialog.getId());

        DialogDTO veiledersDialog = veileder.createRequest()
                .body(veiledersHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    public void nyHenvendelse_brukerSvarerPaaVeiledersHenvendelse_venterPaNav() {

        NyHenvendelseDTO veiledersHenvendelse = new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift");
        DialogDTO veiledersDialog = veileder.createRequest()
                .body(veiledersHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();

        NyHenvendelseDTO brukersHenvendelse = new NyHenvendelseDTO().setTekst("tekst").setDialogId(veiledersDialog.getId());
        bruker.createRequest()
                .body(brukersHenvendelse)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200);

        veiledersDialog = veileder.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", veiledersDialog.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();
    }

    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnes_oppdatererEgenskap() {
        final String aktivitetId = "123";
        var henvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setAktivitetId(aktivitetId);

        veileder.createRequest()
                .body(henvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200);

        List<DialogDTO> opprettetDialog = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);


        assertThat(opprettetDialog.get(0).getEgenskaper()).isEmpty();
        assertThat(opprettetDialog).hasSize(1);

//forhandsorientering
        veileder.createRequest()
                .body(henvendelse)
                .post("/veilarbdialog/api/dialog/forhandsorientering?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200);

        List<DialogDTO> dialogMedParagraf8 = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

        assertThat(dialogMedParagraf8.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
        assertThat(dialogMedParagraf8).hasSize(1);
    }

    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnesIkke_oppdatererEgenskap() {

        veileder.createRequest()
                .body(new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId("123"))
                .post("/veilarbdialog/api/dialog/forhandsorientering?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200);

        val hentedeDialoger = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);
        assertThat(hentedeDialoger.size()).isEqualTo(1);
        assertThat(hentedeDialoger.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
    }
}
