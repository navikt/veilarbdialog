package no.nav.fo.veilarbdialog.internapi;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.util.DialogTestService;
import no.nav.veilarbdialog.internapi.model.Dialog;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class InternApiControllerTest {

    @LocalServerPort
    protected int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DialogTestService dialogTestService;

    @After
    public void cleanUp() {
        jdbcTemplate.update("delete from HENVENDELSE");
        jdbcTemplate.update("delete from DIALOG");
    }

    @Test
    public void hentDialoger() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        DialogDTO opprettetDialog = dialogTestService.opprettDialogSomVeileder(port, mockVeileder, mockBruker, new NyHenvendelseDTO().setTekst("tekst"));

        Dialog dialog = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog/{dialogId}", opprettetDialog.getId())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .as(Dialog.class);

        SoftAssertions.assertSoftly(d -> {
            d.assertThat(dialog.getAktivitetId()).isEqualTo(null);
            d.assertThat(Date.from(dialog.getOpprettetDato().toInstant())).isEqualTo(opprettetDialog.getOpprettetDato());
            d.assertThat(dialog.getHenvendelser().get(0).getTekst()).isEqualTo(opprettetDialog.getHenvendelser().get(0).getTekst());
            d.assertThat(dialog.getHenvendelser().get(0).getLestAvBruker()).isEqualTo(false);
            d.assertThat(dialog.getHenvendelser().get(0).getLestAvVeileder()).isEqualTo(true);
            d.assertAll();
        });

        dialogTestService.opprettDialogSomBruker(port, mockBruker, new NyHenvendelseDTO().setTekst("tekst2"));

        // Sett bruker under KVP
        BrukerOptions kvpOptions = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        MockNavService.updateBruker(mockBruker, kvpOptions);
        dialogTestService.opprettDialogSomBruker(port, mockBruker, new NyHenvendelseDTO().setTekst("tekst3"));

        List<Dialog> dialoger = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getList(".", Dialog.class);
        assertThat(dialoger).hasSize(2);

        List<Dialog> dialoger2 = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog?oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getList(".", Dialog.class);
        assertThat(dialoger2).hasSameElementsAs(dialoger);

        List<Dialog> dialoger3 = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}&oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getAktorId(),
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getList(".", Dialog.class);
        assertThat(dialoger3).hasSameElementsAs(dialoger);
    }

    @Test
    public void skalFeilNaarManglerParameter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
        mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog")
                .then()
                .assertThat().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void skalFeilNaarEksternBruker() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        mockBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}",
                        mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void skalFeileNaarManglerTilgang() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeilederUtenBruker = MockNavService.createVeileder();

        dialogTestService.opprettDialogSomBruker(port, mockBruker, new NyHenvendelseDTO().setTekst("tekst"));

        mockVeilederUtenBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}",
                        mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());

        mockVeilederUtenBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog?oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());

        mockVeilederUtenBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}&oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getAktorId(),
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }
}