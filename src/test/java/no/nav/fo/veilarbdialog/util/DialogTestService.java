package no.nav.fo.veilarbdialog.util;

import io.restassured.response.Response;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.mock_nav_modell.RestassuredUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static org.junit.Assert.assertNotNull;

@Service
public class DialogTestService {

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    public DialogDTO opprettDialogSomBruker(int port, MockBruker bruker, NyHenvendelseDTO nyHenvendelseDTO) {
        return opprettDialog(port, bruker, bruker, nyHenvendelseDTO);
    }

    public DialogDTO opprettDialogSomVeileder(int port, MockVeileder veileder, MockBruker bruker, NyHenvendelseDTO nyHenvendelseDTO) {
        return opprettDialog(port, veileder, bruker, nyHenvendelseDTO);
    }

    public DialogDTO hentDialog(int port, RestassuredUser restassuredUser, long dialogId) {
        return restassuredUser.createRequest()
                .port(port)
                .get("/veilarbdialog/api/dialog/{dialogId}", dialogId)
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
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

    public EskaleringsvarselDto startEskalering(MockVeileder veileder, StartEskaleringDto startEskaleringDto) {
        Response response = veileder.createRequest()
                .body(startEskaleringDto)
                .when()
                .post("/veilarbdialog/api/eskaleringsvarsel/start")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();
        EskaleringsvarselDto eskaleringsvarselDto = response.as(EskaleringsvarselDto.class);
        assertNotNull(eskaleringsvarselDto);
        // Scheduled task
        brukernotifikasjonService.sendPendingBrukernotifikasjoner();
        return eskaleringsvarselDto;
    }
}
