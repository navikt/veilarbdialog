package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class KladdRessursTest extends SpringBootTestBase  {

    private MockBruker bruker;
    private MockVeileder veileder;

    @BeforeEach
    void setupl() { // Må bruke et annet navn en "setup" fordi det brukes i super-klassen
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
        Mockito.when(unleash.isEnabled("veilarbdialog.dialogvarsling")).thenReturn(true);
    }

    @Test
    void skal_kunne_lage_kladd_med_fnr_i_url() {
        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");
        var traad = dialogTestService.opprettDialogSomVeileder(veileder, bruker, nyHenvendelse);
        var kladd = KladdDTO.builder()
                .dialogId(traad.getId())
                .fnr(bruker.getFnr())
                .aktivitetId(traad.getId()) // Workaround
                .tekst("noe").build();
        dialogTestService.nyKladdMedFnrIUrl(veileder, kladd);
    }

    @Test
    void skal_kunne_hente_kladder() {
        NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift");
        var traad = dialogTestService.opprettDialogSomVeileder(veileder, bruker, nyHenvendelse);
        var kladd = KladdDTO.builder()
                .dialogId(traad.getId())
                .fnr(bruker.getFnr())
                .aktivitetId(traad.getId()) // Workaround
                .tekst("noe").build();
        dialogTestService.nyKladdMedFnrIUrl(veileder, kladd);
        var kladder = dialogTestService.hentKladder(veileder, bruker);
        assertThat(kladder).hasSize(1);
    }
}
