package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.config.HttpServletRequestConfig;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = HttpServletRequestConfig.class)

@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = AFTER_TEST_METHOD
)
public class DialogRessursTest {
    final static String fnr = "12345";
    final static String aktorId = "54321";
    final static String veilederIdent = "V123";

    @MockBean
    private AuthService authService;

    @Autowired
    JdbcTemplate jdbc;

    @MockBean
    private KvpService kvpService;

    @Autowired
    private AktorOppslagClient aktorOppslagClient;

    @Autowired
    private DialogRessurs dialogRessurs;

    @Before
    public void before() {
        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn(null);
        when(aktorOppslagClient.hentAktorId(Fnr.of(fnr))).thenReturn(AktorId.of(aktorId));
        when(authService.harTilgangTilPerson(anyString())).thenReturn(true);
    }

    private void mockErEksternBruker() {
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.getIdent()).thenReturn(Optional.of(fnr));


    }
    private void mockErVeileder() {
        when(authService.erInternBruker()).thenReturn(true);
        when(authService.erEksternBruker()).thenReturn(false);
        when(authService.getIdent()).thenReturn(Optional.of(veilederIdent));

    }

    @Test
    public void hentDialoger() {
        mockErEksternBruker();
        dialogRessurs.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        List<DialogDTO> dialoger = dialogRessurs.hentDialoger();
        assertThat(dialoger.size()).isEqualTo(1);
    }

    @Test
    public void nyHenvendelse_fraBruker_venterPaaNav() {
        mockErEksternBruker();
        NyHenvendelseDTO nyHenvendelseDTO = new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift");
        DialogDTO dialog = dialogRessurs.nyHenvendelse(nyHenvendelseDTO);

        //Bruker skal ikke vite om nav har ferdig behandlet dialogen
        assertThat(dialog.isVenterPaSvar()).isFalse();
        assertThat(dialog.isFerdigBehandlet()).isTrue();

        mockErVeileder();
        DialogDTO veiledersDialog = dialogRessurs.hentDialog(dialog.getId());

        assertThat(veiledersDialog.isVenterPaSvar()).isFalse();
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();
    }

    @Test
    public void nyHenvendelse_fraVeileder_venterIkkePaaNoen() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        mockErVeileder();
        NyHenvendelseDTO nyHenvendelseDTO = new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift");

        DialogDTO dialog = dialogRessurs.nyHenvendelse(nyHenvendelseDTO);

        assertThat(dialog.isVenterPaSvar()).isFalse();
        assertThat(dialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    public void nyHenvendelse_veilederSvarerPaaBrukersHenvendelse_venterIkkePaaNav() {

        mockErEksternBruker();
        NyHenvendelseDTO brukersHenvendelse = new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift");
        DialogDTO brukersDialog = dialogRessurs.nyHenvendelse(brukersHenvendelse);

        mockErVeileder();
        NyHenvendelseDTO veiledersHenvendelse = new NyHenvendelseDTO().setTekst("tekst");
        DialogDTO veiledersDialog = dialogRessurs.nyHenvendelse(veiledersHenvendelse.setDialogId(brukersDialog.getId()));

        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();
    }

    @Test
    public void nyHenvendelse_brukerSvarerPaaVeiledersHenvendelse_venterPaNav() {

        mockErVeileder();
        NyHenvendelseDTO veiledersHenvendelse = new NyHenvendelseDTO().setTekst("tekst").setOverskrift("overskrift");
        DialogDTO veiledersDialog = dialogRessurs.nyHenvendelse(veiledersHenvendelse);

        assertThat(veiledersDialog.isFerdigBehandlet()).isTrue();

        mockErEksternBruker();
        NyHenvendelseDTO brukersHenvendelse = new NyHenvendelseDTO().setTekst("tekst");
        dialogRessurs.nyHenvendelse(brukersHenvendelse.setDialogId(veiledersDialog.getId()));

        mockErVeileder();
        veiledersDialog = dialogRessurs.hentDialog(veiledersDialog.getId());
        assertThat(veiledersDialog.isFerdigBehandlet()).isFalse();
    }

    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnes_oppdatererEgenskap() {
        final String aktivitetId = "123";
        var henvendelse = new NyHenvendelseDTO()
                .setTekst("tekst")
                .setAktivitetId(aktivitetId);

        mockErEksternBruker();
        dialogRessurs.nyHenvendelse(henvendelse);

        val opprettetDialog = dialogRessurs.hentDialoger();
        assertThat(opprettetDialog.get(0).getEgenskaper().isEmpty()).isTrue();
        assertThat(opprettetDialog.size()).isEqualTo(1);

        dialogRessurs.forhandsorienteringPaAktivitet(henvendelse);

        val dialogMedParagraf8 = dialogRessurs.hentDialoger();
        assertThat(dialogMedParagraf8.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
        assertThat(dialogMedParagraf8.size()).isEqualTo(1);
    }

    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnesIkke_oppdatererEgenskap() {
        mockErEksternBruker();
        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId("123")
        );

        val hentedeDialoger = dialogRessurs.hentDialoger();
        assertThat(hentedeDialoger.size()).isEqualTo(1);
        assertThat(hentedeDialoger.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
    }
}
