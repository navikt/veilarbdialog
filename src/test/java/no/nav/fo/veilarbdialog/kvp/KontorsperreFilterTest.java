
package no.nav.fo.veilarbdialog.kvp;

import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class KontorsperreFilterTest {

    private KontorsperreFilter filter;
    private AuthService auth;

    @Before
    public void setUp() {

        auth = mock(AuthService.class);
        filter = new KontorsperreFilter(auth);

    }

    @Test
    public void tilgangTilEnhet_kontorsperreEnhetErNull_returnererTrue() {

        assertThat(filter.tilgangTilEnhet(HenvendelseData.builder().kontorsperreEnhetId(null).build())).isTrue();
        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId(null).build())).isTrue();

    }

    @Test
    public void tilgangTilEnhet_kontorsperreEnhetErTom_returnererTrue() {

        assertThat(filter.tilgangTilEnhet(HenvendelseData.builder().kontorsperreEnhetId("").build())).isTrue();
        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId("").build())).isTrue();

    }

    @Test
    public void tilgangTilEnhet_veilederHarTilgangIABAC_skalReturnereTrue() {
        String enhet = "enhet";
        String veileder = "veileder";

        when(auth.erEksternBruker()).thenReturn(false);
        when(auth.getIdent()).thenReturn(Optional.of(veileder));
        when(auth.harVeilederTilgangTilEnhet(veileder, enhet))
                .thenReturn(true);

        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId(enhet).build())).isTrue();

    }

    @Test
    public void tilgangTilEnhet_veilederHarIkkeTilgangIABAC_skalReturnereFalse() {
        String enhet = "enhet";
        String veileder = "veileder";

        when(auth.erEksternBruker()).thenReturn(false);
        when(auth.getIdent()).thenReturn(Optional.of(veileder));
        when(auth.harVeilederTilgangTilEnhet(veileder, enhet))
                .thenReturn(false);
        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId(enhet).build())).isFalse();

    }

}
