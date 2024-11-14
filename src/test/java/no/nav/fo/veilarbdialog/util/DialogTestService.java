package no.nav.fo.veilarbdialog.util;

import io.restassured.response.Response;
import no.nav.fo.veilarbdialog.brukernotifikasjon.MinsideVarselService;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.mock_nav_modell.RestassuredUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Service
public class DialogTestService {

    @Autowired
    MinsideVarselService minsideVarselService;

    public DialogDTO opprettDialogSomBruker(MockBruker bruker, NyMeldingDTO nyHenvendelseDTO) {
        return opprettDialog(bruker, bruker, nyHenvendelseDTO);
    }

    public DialogDTO opprettDialogSomVeileder(MockVeileder veileder, MockBruker bruker, NyMeldingDTO nyHenvendelseDTO) {
        return opprettDialog(veileder, bruker, nyHenvendelseDTO);
    }

    public DialogDTO hentDialog(RestassuredUser restassuredUser, long dialogId) {
        return restassuredUser.createRequest()
                .get("/veilarbdialog/api/dialog/{dialogId}", dialogId)
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
    }

    private DialogDTO opprettDialog(RestassuredUser restassuredUser, MockBruker bruker, NyMeldingDTO nyHenvendelseDTO) {
        Response response = restassuredUser.createRequest()
                .body(nyHenvendelseDTO)
                .when()
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
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
        minsideVarselService.sendPendingVarsler();
        return eskaleringsvarselDto;
    }

    public void nyKladd(RestassuredUser user, KladdDTO kladd) {
        user.createRequest()
                .body(kladd)
                .post("/veilarbdialog/api/kladd")
                .then()
                .statusCode(204);
    }

    public void nyKladdMedFnrIUrl(RestassuredUser user, KladdDTO kladd) {
        var fnr = kladd.getFnr();
        user.createRequest()
                .body(kladd.setFnr(null))
                .post("/veilarbdialog/api/kladd?fnr=" + fnr)
                .then()
                .statusCode(204);
    }

    public List<KladdDTO> hentKladder(RestassuredUser user, MockBruker bruker) {
        return user.createRequest()
                .get("/veilarbdialog/api/kladd?fnr=" + bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", KladdDTO.class);
    }
        public void stoppEskalering(MockVeileder veileder, StopEskaleringDto stopEskaleringDto) {
            veileder.createRequest()
                    .body(stopEskaleringDto)
                    .when()
                    .patch("/veilarbdialog/api/eskaleringsvarsel/stop")
                    .then()
                    .assertThat().statusCode(HttpStatus.OK.value())
                    .extract().response();
            minsideVarselService.sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes();
    }
}
