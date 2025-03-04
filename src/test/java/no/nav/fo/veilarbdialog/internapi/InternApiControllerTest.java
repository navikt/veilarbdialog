package no.nav.fo.veilarbdialog.internapi;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.veilarbdialog.internapi.model.Dialog;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class InternApiControllerTest extends SpringBootTestBase {

    @Test
    void hentDialoger() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        DialogDTO dialog1 = dialogTestService.opprettDialogSomVeileder(mockVeileder, mockBruker, new NyMeldingDTO().setTekst("dialog 1 - henvendelse 1"));

        Dialog hentetDialog1 = mockVeileder.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog/{dialogId}", dialog1.getId())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .as(Dialog.class);

        SoftAssertions.assertSoftly(d -> {
            d.assertThat(hentetDialog1.getAktivitetId()).isEqualTo(null);
            d.assertThat(Date.from(hentetDialog1.getOpprettetDato().toInstant())).isEqualTo(dialog1.getOpprettetDato());
            d.assertThat(hentetDialog1.getHenvendelser().getFirst().getTekst()).isEqualTo(dialog1.getHenvendelser().getFirst().getTekst());
            d.assertThat(hentetDialog1.getHenvendelser().getFirst().getLestAvBruker()).isEqualTo(false);
            d.assertThat(hentetDialog1.getHenvendelser().getFirst().getLestAvVeileder()).isEqualTo(true);
            d.assertAll();
        });

        DialogDTO dialog2 = dialogTestService.opprettDialogSomBruker(mockBruker, new NyMeldingDTO().setTekst("dialog 2 - henvendelse 1"));

        // Sett bruker under KVP
        BrukerOptions kvpOptions = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        MockNavService.updateBruker(mockBruker, kvpOptions);
        dialogTestService.opprettDialogSomBruker(mockBruker, new NyMeldingDTO().setTekst("dialog3 - henvendelse 1"));

        // Opprett henvendelse/melding med kontorsperre på en dialog uten kontorsperre
        dialogTestService.opprettDialogSomBruker(mockBruker, new NyMeldingDTO().setTekst("dialog 2 - henvendelse 2 kvp").setDialogId(dialog2.getId()));

        // Veileder med tilgang til mockbrukers enhet
        List<Dialog> dialoger = mockVeileder.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getList(".", Dialog.class);
        assertThat(dialoger).hasSize(3); // 3 dialoger totalt

        Dialog hentetDialog2MedTilgang = dialoger.stream().filter( d -> Objects.equals(d.getDialogId(), dialog2.getId())).findFirst().get(); // hent dialog2
        assertThat(hentetDialog2MedTilgang.getHenvendelser()).hasSize(2); // sjekk at dialog2 har 2 henvendelser

        // Veileder uten tilgang til mockbrukers enhet
        MockVeileder veilederUtenTilgangTilEnhet = MockNavService.createNKS();
        List<Dialog> dialogListe2 = veilederUtenTilgangTilEnhet.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getList(".", Dialog.class);
        assertThat(dialogListe2).hasSize(2);
        Dialog hentetDialog2UtenTilgang = dialogListe2.stream().filter( d -> Objects.equals(d.getDialogId(), dialog2.getId())).findFirst().get();
        assertThat(hentetDialog2UtenTilgang.getHenvendelser()).hasSize(1); // Forventer kun 1 henvendelse på dialog 2

        Dialog hentetDialog2UtenTilgangViaDirekteURL = veilederUtenTilgangTilEnhet.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog/{dialogId}", dialog2.getId())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .as(Dialog.class);
        assertThat(hentetDialog2UtenTilgangViaDirekteURL.getHenvendelser()).hasSize(1);

        // Test request parameter(e)
        List<Dialog> dialoger3 = mockVeileder.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getList(".", Dialog.class);
        assertThat(dialoger3).hasSameElementsAs(dialoger);

        List<Dialog> dialoger4 = mockVeileder.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}&oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getAktorId(),
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getList(".", Dialog.class);
        assertThat(dialoger4).hasSameElementsAs(dialoger);
    }

    @Test
    void skalFeilNaarManglerParameter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
        mockVeileder.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog")
                .then()
                .assertThat().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void skalFeilNaarEksternBruker() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        mockBruker.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}",
                        mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void skalFeileNaarManglerTilgang() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeilederUtenBruker = MockNavService.createVeileder();

        dialogTestService.opprettDialogSomBruker(mockBruker, new NyMeldingDTO().setTekst("tekst"));

        mockVeilederUtenBruker.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}",
                        mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());

        mockVeilederUtenBruker.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());

        mockVeilederUtenBruker.createRequest()
                .get("http://localhost/veilarbdialog/internal/api/v1/dialog?aktorId={aktorId}&oppfolgingsperiodeId={oppfolgingsperiodeId}",
                        mockBruker.getAktorId(),
                        mockBruker.getOppfolgingsperiode().toString())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }
}
