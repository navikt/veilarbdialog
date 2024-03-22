package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;


class DialogRessursTest extends SpringBootTestBase {

    private MockBruker bruker;
    private MockVeileder veileder;

    @BeforeEach
    void setupl() {
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
        Mockito.when(unleash.isEnabled("veilarbdialog.dialogvarsling")).thenReturn(true);
    }

    @Test
    void hentDialoger_bruker() {
        veileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst"))
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
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
    void skal_kunne_filtrere_vekk_dialoger_med_kontorsperre_på_veileders_egen_enhet_når_bruker_er_i_kvp() {
        var oppfølgingsenhet = "enhetTilKvpBruker";
        var brukerOptions = BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).harBruktNivaa4(true).erManuell(false).kanVarsles(true).oppfolgingsEnhet(oppfølgingsenhet).build();
        var kvpBruker = MockNavService.createBruker(brukerOptions);
        var veileder = MockNavService.createVeileder(kvpBruker);
        veileder.setNasjonalTilgang(true);
        dialogTestService.opprettDialogSomVeileder(veileder, kvpBruker, new NyHenvendelseDTO().setTekst("hei"));
        dialogTestService.opprettDialogSomBruker(kvpBruker, new NyHenvendelseDTO().setTekst("hallo"));

        var dialoger = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}&ekskluderDialogerMedKontorsperre=true", kvpBruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

        assertThat(dialoger).isEmpty();
    }

    @Test
    void skal_kunne_inkludere_dialoger_med_kontorsperre_på_veileders_egen_enhet_selv_om_bruker_er_i_kvp() {
        var oppfølgingsenhet = "enhetTilKvpBruker";
        var brukerOptions = BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).harBruktNivaa4(true).erManuell(false).kanVarsles(true).oppfolgingsEnhet(oppfølgingsenhet).build();
        var kvpBruker = MockNavService.createBruker(brukerOptions);
        var veileder = MockNavService.createVeileder(kvpBruker);
        dialogTestService.opprettDialogSomVeileder(veileder, kvpBruker, new NyHenvendelseDTO().setTekst("hei"));
        dialogTestService.opprettDialogSomBruker(kvpBruker, new NyHenvendelseDTO().setTekst("hallo"));

        var dialoger = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}", kvpBruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

        assertThat(dialoger).hasSize(2);
    }

    @Test
    void nyHenvendelse_fraBruker_venterPaaNav() {
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
        verify(postRequestedFor(urlEqualTo("/please/notify-subscribers"))
                .withRequestBody(matchingJsonPath("eventType", equalTo(DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV.name()))));
    }

    @Test
    void nyHenvendelse_fraVeileder_venterIkkePaaNoen() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        DialogDTO dialog = veileder.createRequest()
                .body(new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift"))
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(dialog.isVenterPaSvar()).isFalse();
        assertThat(dialog.isFerdigBehandlet()).isTrue();
        verify(postRequestedFor(urlEqualTo("/please/notify-subscribers"))
                .withRequestBody(matchingJsonPath("eventType", equalTo(DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER.name()))));
    }

    @Test
    void nyHenvendelse_veilederSvarerPaaBrukersHenvendelse_venterIkkePaaNav() {

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
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    void nyHenvendelse_brukerSvarerPaaVeiledersHenvendelse_venterPaNav() {

        NyHenvendelseDTO veiledersHenvendelse = new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift");
        DialogDTO veiledersDialog = veileder.createRequest()
                .body(veiledersHenvendelse)
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
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
    void nyHenvendelse_egenvurdering_venterIkkePaaSvarFraNav() {

        NyHenvendelseDTO egenVurdering = new NyHenvendelseDTO()
                .setTekst("Jeg skal klare meg selv")
                .setOverskrift("Egenvurdering")
                .setVenterPaaSvarFraNav(false);
        DialogDTO brukersEgenvurdering = bruker.createRequest()
                .body(egenVurdering)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        DialogDTO veiledersDialog = veileder.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", brukersEgenvurdering.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    void nyHenvendelse_egenvurdering_venterPaaSvarFraNav() {

        NyHenvendelseDTO egenVurdering = new NyHenvendelseDTO()
                .setTekst("Jeg trenger hjelp fra Nav")
                .setOverskrift("Egenvurdering")
                .setVenterPaaSvarFraNav(true);
        DialogDTO brukersEgenvurdering = bruker.createRequest()
                .body(egenVurdering)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        DialogDTO veiledersDialog = veileder.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", brukersEgenvurdering.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();

    }

    @Test
    void nyHenvendelse_egenvurdering_venterPaaSvarFraNav_default() {

        NyHenvendelseDTO egenVurdering = new NyHenvendelseDTO()
                .setOverskrift("Egenvurdering")
                .setTekst("Jeg trenger hjelp fra Nav");
        DialogDTO brukersEgenvurdering = bruker.createRequest()
                .body(egenVurdering)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        DialogDTO veiledersDialog = veileder.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", brukersEgenvurdering.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();

    }

    @Test
    void nyHenvendelse_fraVeileder_kanVentePaaBeggeParter() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        DialogDTO dialog = veileder.createRequest()
                .body(
                        new NyHenvendelseDTO()
                                .setTekst("tekst")
                                .setOverskrift("overskrift")
                                .setVenterPaaSvarFraBruker(Boolean.TRUE)
                                .setVenterPaaSvarFraNav(Boolean.TRUE)
                )
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(dialog.isVenterPaSvar()).isTrue();
        assertThat(dialog.isFerdigBehandlet()).isFalse();
    }


    @Test
    void forhandsorienteringPaAktivitet_dialogFinnes_oppdatererEgenskap() {
        final String aktivitetId = "123";
        var henvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setAktivitetId(aktivitetId);

        veileder.createRequest()
                .body(henvendelse)
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200);

        List<DialogDTO> opprettetDialog = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
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
                .post("/veilarbdialog/api/dialog/forhandsorientering?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200);

        List<DialogDTO> dialogMedParagraf8 = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);

        assertThat(dialogMedParagraf8.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
        assertThat(dialogMedParagraf8).hasSize(1);
    }

    @Test
    void forhandsorienteringPaAktivitet_dialogFinnesIkke_oppdatererEgenskap() {

        veileder.createRequest()
                .body(new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId("123"))
                .post("/veilarbdialog/api/dialog/forhandsorientering?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200);

        var hentedeDialoger = veileder.createRequest()
                .get("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);
        assertThat(hentedeDialoger.size()).isEqualTo(1);
        assertThat(hentedeDialoger.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
    }
}
