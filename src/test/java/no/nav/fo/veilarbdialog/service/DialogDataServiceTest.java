package no.nav.fo.veilarbdialog.service;

import io.restassured.RestAssured;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@RunWith(SpringRunner.class)
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = BEFORE_TEST_METHOD
)
public class DialogDataServiceTest {

    @LocalServerPort
    private int port;

    @Autowired
    JdbcTemplate jdbcTemplate;


    MockBruker bruker;
    MockVeileder brukersVeileder;
    MockVeileder veilederNasjonalTilgang;
    MockVeileder tilfeldigVeileder;

    @Before
    public void setup() {
        RestAssured.port = port;
        bruker = MockNavService.createHappyBruker();
        brukersVeileder = MockNavService.createVeileder(bruker);
        veilederNasjonalTilgang = MockNavService.createVeileder();
        veilederNasjonalTilgang.setNasjonalTilgang(true);
        tilfeldigVeileder = MockNavService.createVeileder();

    }

    @Test
    public void opprettDialog_kontorsperrePaBruker_returnererKontorsperretDialogAvhengigAvTilgang() {

        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");

        DialogDTO dialogUtenomKvp = brukersVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        BrukerOptions brukerOptionsKvp = bruker.getBrukerOptions().toBuilder().erUnderKvp(true).kontorsperreEnhet("1234").build();
        MockNavService.updateBruker(bruker, brukerOptionsKvp);

        DialogDTO dialogUnderKvp = brukersVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        List<DialogDTO> synligeDialogerForVeilederMedNasjonalTilgang = veilederNasjonalTilgang.createRequest()
                .get("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

        assertThat(synligeDialogerForVeilederMedNasjonalTilgang).hasSize(1).containsOnly(dialogUtenomKvp);

        List<DialogDTO> synligeDialogerForBrukersVeileder = brukersVeileder.createRequest()
                .get("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);
        assertThat(synligeDialogerForBrukersVeileder).hasSize(2);
    }

    @Test
    public void opprettHenvendelse_brukerManglerTilgangTilPerson_Forbidden403() {

        BrukerOptions brukerOptionsKvp = bruker.getBrukerOptions().toBuilder().erUnderKvp(true).kontorsperreEnhet("1234").build();
        MockNavService.updateBruker(bruker, brukerOptionsKvp);

        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");


        tilfeldigVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(403);
    }

    @Test
    public void publicMetoder_sjekkerOmBrukerHarTilgang() {

        BrukerOptions brukerOptionsKvp = bruker.getBrukerOptions().toBuilder().erUnderKvp(true).kontorsperreEnhet("1234").build();
        MockNavService.updateBruker(bruker, brukerOptionsKvp);

        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift")
                .setAktivitetId("12345");

        DialogDTO dialogUnderKvp = brukersVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);


        tilfeldigVeileder.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", dialogUnderKvp.getId())
                .then()
                .statusCode(403);

        tilfeldigVeileder.createRequest()
                .get("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .statusCode(403);

        tilfeldigVeileder.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialogUnderKvp.getId())
                .then()
                .statusCode(403);

    }

}
