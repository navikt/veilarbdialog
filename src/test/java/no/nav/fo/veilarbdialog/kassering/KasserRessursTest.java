package no.nav.fo.veilarbdialog.kassering;

import io.restassured.response.ValidatableResponse;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.mock_nav_modell.RestassuredUser;
import org.junit.jupiter.api.Test;

class KasserRessursTest extends SpringBootTestBase {


    MockBruker mockBruker = MockNavService.createHappyBruker();
    MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
    @Test
    void bruker_skal_ikke_kunne_kassere() {
        NyHenvendelseDTO nyHenvendelseDTO = new NyHenvendelseDTO().setTekst("Lol");
        DialogDTO opprettetDialog = dialogTestService.opprettDialogSomBruker(mockBruker, nyHenvendelseDTO);
        kasserDialog(mockBruker, Long.parseLong(opprettetDialog.getId()))
                .statusCode(403);
    }

    @Test
    void veileder_som_ikke_er_lista_skal_ikke_kunne_kassere() {
        NyHenvendelseDTO nyHenvendelseDTO = new NyHenvendelseDTO().setTekst("Lol");
        DialogDTO opprettetDialog = dialogTestService.opprettDialogSomBruker(mockBruker, nyHenvendelseDTO);
        kasserDialog(mockVeileder, Long.parseLong(opprettetDialog.getId()))
                .statusCode(403);
    }

    ValidatableResponse kasserDialog(RestassuredUser restassuredUser, long dialogId) {
        return restassuredUser.createRequest()
                .put("/veilarbdialog/api/kassering/dialog/{dialogId}/kasser", dialogId).then();
    }

}