package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.feed.KvpService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class DialogRessursTest {
    final static String fnr = "12345";
    final static String aktorId = "54321";

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
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.harTilgangTilPerson(anyString())).thenReturn(true);
        when(authService.getIdent()).thenReturn(Optional.of(fnr));
    }

    @After
    public void after() {
        jdbc.update("delete from HENVENDELSE");
        jdbc.update("delete from DIALOG_EGENSKAP");
        jdbc.update("delete from DIALOG");

    }

    public void hentDialoger() {
        dialogRessurs.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        List<DialogDTO> dialoger = dialogRessurs.hentDialoger();
        assertThat(dialoger.size()).isEqualTo(1);
    }


    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnes_oppdatererEgenskap() {
        final String aktivitetId = "123";

        dialogRessurs.nyHenvendelse(
                new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId(aktivitetId)
        );

        val opprettetDialog = dialogRessurs.hentDialoger();
        assertThat(opprettetDialog.get(0).getEgenskaper().isEmpty()).isTrue();
        assertThat(opprettetDialog.size()).isEqualTo(1);

        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("tekst")
                        .setAktivitetId(aktivitetId)
        );

        val dialogMedParagraf8 = dialogRessurs.hentDialoger();
        assertThat(dialogMedParagraf8.get(0).getEgenskaper()).contains(Egenskap.PARAGRAF8);
        assertThat(dialogMedParagraf8.size()).isEqualTo(1);
    }

    @Test
    public void forhandsorienteringPaAktivitet_dialogFinnesIkke_oppdatererEgenskap() {
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
