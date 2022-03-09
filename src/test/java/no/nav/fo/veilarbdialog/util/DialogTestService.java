package no.nav.fo.veilarbdialog.util;

import io.restassured.response.Response;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.mock_nav_modell.RestassuredUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static org.junit.Assert.assertNotNull;

@Service
public class DialogTestService {

    public DialogDTO opprettDialogSomBruker(int port, MockBruker bruker, NyHenvendelseDTO nyHenvendelseDTO) {
        return opprettDialog(port, bruker, bruker, nyHenvendelseDTO);
    }

    public DialogDTO opprettDialogSomVeileder(int port, MockVeileder veileder, MockBruker bruker, NyHenvendelseDTO nyHenvendelseDTO) {
        return opprettDialog(port, veileder, bruker, nyHenvendelseDTO);
    }

    private DialogDTO opprettDialog(int port, RestassuredUser restassuredUser, MockBruker bruker, NyHenvendelseDTO nyHenvendelseDTO) {
        Response response = restassuredUser.createRequest()
                .port(port)
                .body(nyHenvendelseDTO)
                .when()
                .post("/veilarbdialog/api/dialog?aktorId={aktorId}", bruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        DialogDTO dialog = response.as(DialogDTO.class);
        assertNotNull(dialog);
        assertNotNull(dialog.getId());
        return dialog;
    }


}
