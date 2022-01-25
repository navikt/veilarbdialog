package no.nav.fo.veilarbdialog.service;

import io.restassured.RestAssured;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import no.nav.fo.veilarbdialog.mock_nav_modell.*;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService;
import org.assertj.core.api.Assertions;
import org.junit.*;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@RunWith(SpringRunner.class)
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

    @After
    public void cleanUp() {
        jdbcTemplate.update("delete from HENVENDELSE");
        jdbcTemplate.update("delete from DIALOG");
        jdbcTemplate.update("delete from DIALOG_AKTOR");
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
