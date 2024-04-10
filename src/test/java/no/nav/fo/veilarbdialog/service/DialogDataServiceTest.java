package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class DialogDataServiceTest extends SpringBootTestBase {

    MockBruker bruker;
    MockVeileder brukersVeileder;
    MockVeileder veilederNasjonalTilgang;
    MockVeileder tilfeldigVeileder;

    @BeforeEach
    void setupL() {
        bruker = MockNavService.createHappyBruker();
        brukersVeileder = MockNavService.createVeileder(bruker);
        veilederNasjonalTilgang = MockNavService.createNKS();
        tilfeldigVeileder = MockNavService.createVeileder();
    }

    @Test
    void opprettDialog_kontorsperrePaBruker_returnererKontorsperretDialogAvhengigAvTilgang() {

        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");

        DialogDTO dialogUtenomKvp = brukersVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        BrukerOptions brukerOptionsKvp = bruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        MockNavService.updateBruker(bruker, brukerOptionsKvp);

        DialogDTO dialogUnderKvp = brukersVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        List<DialogDTO> synligeDialogerForVeilederMedNasjonalTilgang = veilederNasjonalTilgang.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

        assertThat(synligeDialogerForVeilederMedNasjonalTilgang).hasSize(1).containsOnly(dialogUtenomKvp);

        List<DialogDTO> synligeDialogerForBrukersVeileder = brukersVeileder.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);
        assertThat(synligeDialogerForBrukersVeileder).hasSize(2);
    }

    @Test
    void opprettHenvendelse_brukerManglerTilgangTilPerson_Forbidden403() {

        BrukerOptions brukerOptionsKvp = bruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        MockNavService.updateBruker(bruker, brukerOptionsKvp);

        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");


        tilfeldigVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(403);
    }

    @Test
    void publicMetoder_sjekkerOmBrukerHarTilgang() {

        BrukerOptions brukerOptionsKvp = bruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        MockNavService.updateBruker(bruker, brukerOptionsKvp);

        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift")
                .setAktivitetId("12345");

        DialogDTO dialogUnderKvp = brukersVeileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);


        tilfeldigVeileder.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", dialogUnderKvp.getId())
                .then()
                .statusCode(403);

        tilfeldigVeileder.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(403);

        tilfeldigVeileder.createRequest()
                .put("/veilarbdialog/api/dialog/{dialogId}/les", dialogUnderKvp.getId())
                .then()
                .statusCode(403);

    }

}
