package no.nav.fo.veilarbdialog.rest;

import io.restassured.response.Response;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.mock_nav_modell.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;
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

    private void veilederSenderMelding() {
        nyMelding(veileder, bruker);
    }

    private DialogDTO nyMelding(RestassuredUser avsender, MockBruker bruker) {
        return nyMelding(avsender, bruker, new NyMeldingDTO().setTekst("tekst"));
    }
    private DialogDTO nyMelding(RestassuredUser avsender, MockBruker bruker, NyMeldingDTO henvendelseDTO) {
        var erVeileder = avsender instanceof MockVeileder;
        var postfix = erVeileder ? "?fnr={fnr}" : null;
        var request = avsender.createRequest()
                .body(henvendelseDTO);
        Response requestWithBody = null;
        if (erVeileder) {
            requestWithBody = request.post("/veilarbdialog/api/dialog" + postfix, bruker.getFnr());
        } else {
            requestWithBody = request.post("/veilarbdialog/api/dialog");
        }
         return  requestWithBody
                 .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
    }

    private DialogDTO nyHenvendelseUtenFnrIUrl(RestassuredUser avsender, NyMeldingDTO henvendelseDTO) {
        return avsender.createRequest()
                .body(henvendelseDTO)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
    }

    private List<DialogDTO> hentDialoger(RestassuredUser avsender, MockBruker mockBruker) {
        var erVeileder = avsender instanceof MockVeileder;
        var postfix = erVeileder ? "?fnr={fnr}" : null;
        var r = avsender.createRequest();
        Response request = null;
        if (erVeileder) {
            request = r.get("/veilarbdialog/api/dialog" + postfix, mockBruker.getFnr());
        } else {
            request = r.get("/veilarbdialog/api/dialog");
        }
        return request
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", DialogDTO.class);
    }

    private DialogDTO hentDialog(RestassuredUser subject, String dialogId) {
        return subject.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", dialogId)
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
    }

    private AntallUlesteDTO hentAntallUleste(RestassuredUser user, String fnr) {
        var request = user.createRequest();
        if (fnr != null) {
            request.body(new FnrDto(fnr));
        }
        return request
                .post("/veilarbdialog/api/dialog/antallUleste")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getObject(".", AntallUlesteDTO.class);
    }

    private SistOppdatertDTO hentSistOppdatert(RestassuredUser user, String fnr) {
        var request = user.createRequest();
        if (fnr != null) {
            request.body(new FnrDto(fnr));
        }
        return request
                .post("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getObject(".", SistOppdatertDTO.class);
    }

    @Test
    void hentDialoger_bruker() {
        veilederSenderMelding();
        var dialoger = hentDialoger(bruker, bruker);
        assertThat(dialoger).hasSize(1);
    }

    @Test
    void bruker_skal_kunne_hente_antall_uleste() {
        veilederSenderMelding();
        var uleste = hentAntallUleste(bruker, null);
        assertThat(uleste.antallUleste).isEqualTo(1);
    }

    @Test
    void veileder_skal_kunne_hente_antall_uleste() {
        veilederSenderMelding();
        var uleste = hentAntallUleste(veileder, bruker.getFnr());
        assertThat(uleste.antallUleste).isZero();
    }

    @Test
    void bruker_skal_kunne_hente_sist_oppdatert() {
        veilederSenderMelding();
        hentSistOppdatert(bruker, null);
    }

    @Test
    void veileder_skal_kunne_hente_sist_oppdatert() {
        veilederSenderMelding();
        hentSistOppdatert(veileder, bruker.getFnr());
    }

    @Test
    void skal_kunne_filtrere_vekk_dialoger_med_kontorsperre_på_veileders_egen_enhet_når_bruker_er_i_kvp() {
        var oppfølgingsenhet = "enhetTilKvpBruker";
        var brukerOptions = BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).erManuell(false).kanVarsles(true).oppfolgingsEnhet(oppfølgingsenhet).build();
        var kvpBruker = MockNavService.createBruker(brukerOptions);
        var veileder = MockNavService.createVeileder(kvpBruker);
        veileder.setNasjonalTilgang(true);
        dialogTestService.opprettDialogSomVeileder(veileder, kvpBruker, new NyMeldingDTO().setTekst("hei"));
        dialogTestService.opprettDialogSomBruker(kvpBruker, new NyMeldingDTO().setTekst("hallo"));

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
        var brukerOptions = BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).erManuell(false).kanVarsles(true).oppfolgingsEnhet(oppfølgingsenhet).build();
        var kvpBruker = MockNavService.createBruker(brukerOptions);
        var veileder = MockNavService.createVeileder(kvpBruker);
        dialogTestService.opprettDialogSomVeileder(veileder, kvpBruker, new NyMeldingDTO().setTekst("hei"));
        dialogTestService.opprettDialogSomBruker(kvpBruker, new NyMeldingDTO().setTekst("hallo"));

        var dialoger = hentDialoger(veileder, kvpBruker);

        assertThat(dialoger).hasSize(2);
    }

    @Test
    void nyHenvendelse_fraBruker_venterPaaNav() {
        DialogDTO dialog = nyMelding(bruker, bruker);

        //Bruker skal ikke vite om nav har ferdig behandlet dialogen
        assertThat(dialog.isVenterPaSvar()).isFalse();
        assertThat(dialog.isFerdigBehandlet()).isTrue();

        DialogDTO veiledersDialog = hentDialog(veileder, dialog.getId());

        assertThat(veiledersDialog.isVenterPaSvar()).isFalse();
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();
        wireMock.verify(postRequestedFor(urlEqualTo("/please/notify-subscribers"))
                .withRequestBody(matchingJsonPath("eventType", equalTo(DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV.name()))));
    }

    @Test
    void nyHenvendelse_fraVeileder_venterIkkePaaNoen() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        DialogDTO dialog = nyMelding(veileder, bruker, new NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift"));

        assertThat(dialog.isVenterPaSvar()).isFalse();
        assertThat(dialog.isFerdigBehandlet()).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/please/notify-subscribers"))
                .withRequestBody(matchingJsonPath("eventType", equalTo(DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER.name()))));
    }

    @Test
    void nyHenvendelse_fraVeileder_venter_på_svar_fra_bruker(){
        NyMeldingDTO dialog = new NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift").setVenterPaaSvarFraBruker(true).setFnr(bruker.getFnr());

        var dialogIRespons = veileder.createRequest()
                .body(dialog)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);

        assertThat(dialogIRespons.isVenterPaSvar()).isTrue();
    }

    @Test
    void nyHenvendelse_veilederSvarerPaaBrukersHenvendelse_venterIkkePaaNav() {
        DialogDTO brukersDialog = nyMelding(bruker, bruker, new NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift"));
        NyMeldingDTO veiledersHenvendelse = new NyMeldingDTO().setTekst("tekst").setDialogId(brukersDialog.getId());
        DialogDTO veiledersDialog = nyMelding(veileder, bruker, veiledersHenvendelse);
        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    void nyHenvendelse_brukerSvarerPaaVeiledersHenvendelse_venterPaNav() {
        NyMeldingDTO veiledersHenvendelse = new NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift");
        DialogDTO veiledersDialog = nyMelding(veileder, bruker, veiledersHenvendelse);

        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();
        NyMeldingDTO brukersHenvendelse = new NyMeldingDTO().setTekst("tekst").setDialogId(veiledersDialog.getId());
        nyMelding(bruker, bruker, brukersHenvendelse);
        veiledersDialog = hentDialog(veileder, veiledersDialog.getId());
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();
    }

    @Test
    void nyHenvendelse_egenvurdering_venterIkkePaaSvarFraNav() {
        NyMeldingDTO egenVurdering = new NyMeldingDTO()
                .setTekst("Jeg skal klare meg selv")
                .setOverskrift("Egenvurdering")
                .setVenterPaaSvarFraNav(false);
        DialogDTO brukersEgenvurdering = nyMelding(bruker, bruker, egenVurdering);
        DialogDTO veiledersDialog = hentDialog(veileder, brukersEgenvurdering.getId());
        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    void nyHenvendelse_egenvurdering_venterPaaSvarFraNav() {
        NyMeldingDTO egenVurdering = new NyMeldingDTO()
                .setTekst("Jeg trenger hjelp fra Nav")
                .setOverskrift("Egenvurdering")
                .setVenterPaaSvarFraNav(true);
        DialogDTO brukersEgenvurdering = nyMelding(bruker, bruker, egenVurdering);
        DialogDTO veiledersDialog = hentDialog(veileder, brukersEgenvurdering.getId());
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();

    }

    @Test
    void nyHenvendelse_egenvurdering_venterPaaSvarFraNav_default() {
        NyMeldingDTO egenVurdering = new NyMeldingDTO()
                .setOverskrift("Egenvurdering")
                .setTekst("Jeg trenger hjelp fra Nav");
        DialogDTO brukersEgenvurdering = nyMelding(bruker, bruker, egenVurdering);
        DialogDTO veiledersDialog = hentDialog(veileder, brukersEgenvurdering.getId());
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();
    }

    @Test
    void nyHenvendelse_fraVeileder_kanVentePaaBeggeParter() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        DialogDTO dialog = nyMelding(veileder, bruker, new NyMeldingDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift")
                .setVenterPaaSvarFraBruker(Boolean.TRUE)
                .setVenterPaaSvarFraNav(Boolean.TRUE));

        assertThat(dialog.isVenterPaSvar()).isTrue();
        assertThat(dialog.isFerdigBehandlet()).isFalse();
    }

    @Test
    void bruker_skal_kunne_sende_henvedelse_uten_fnr_i_url() {
        var melding = new NyMeldingDTO().setTekst("tekst");
        nyHenvendelseUtenFnrIUrl(bruker, melding);
    }

    @Test
    void veileder_skal_kunne_sende_henvedelse_uten_fnr_i_url() {
        var melding = new NyMeldingDTO().setTekst("tekst").setFnr(bruker.getFnr());
        nyHenvendelseUtenFnrIUrl(veileder, melding);
    }

    @Test
    void veileder_skal_ikke_kunne_sende_henvedelse_til_kvpbruker_uten_tilgang_til_enhet() {
        var oppfølgingsenhet = "enhetTilKvpBruker";
        var brukerOptions = BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).erManuell(false).kanVarsles(true).oppfolgingsEnhet(oppfølgingsenhet).build();
        var kvpBruker = MockNavService.createBruker(brukerOptions);
        var melding = new NyMeldingDTO().setFnr(kvpBruker.getFnr()).setTekst("LOL");
        veileder.createRequest()
                .body(melding)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(403);
    }

    @Test
    void veileder_kan_ikke_sende_henvendelse_på_historisk_dialog() {
        NyMeldingDTO henvendelseFørHistorisk = new NyMeldingDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");
        var dialog = dialogTestService.opprettDialogSomVeileder(veileder, bruker, henvendelseFørHistorisk);
        dialogDataService.settDialogerTilHistoriske(bruker.getAktorId(), dialog.getOppfolgingsperiode());

        var melding = new NyMeldingDTO().setFnr(bruker.getFnr()).setTekst("LOL").setDialogId(dialog.getId());
        veileder.createRequest()
                .body(melding)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(409);
    }

    @Test
    void bruker_kan_ikke_sende_henvendelse_på_historisk_dialog() {
        NyMeldingDTO henvendelseFørHistorisk = new NyMeldingDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");
        var dialog = dialogTestService.opprettDialogSomBruker(bruker, henvendelseFørHistorisk);
        dialogDataService.settDialogerTilHistoriske(bruker.getAktorId(), dialog.getOppfolgingsperiode());

        var melding = new NyMeldingDTO().setFnr(bruker.getFnr()).setTekst("LOL").setDialogId(dialog.getId());
        bruker.createRequest()
                .body(melding)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(409);
    }
}
