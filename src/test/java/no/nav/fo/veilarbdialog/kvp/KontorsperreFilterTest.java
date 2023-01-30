
package no.nav.fo.veilarbdialog.kvp;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KontorsperreFilterTest {

    private KontorsperreFilter filter;
    private IAuthService auth;

    @BeforeEach
    void setUp() {

        auth = mock(IAuthService.class);
        filter = new KontorsperreFilter(auth);

    }

    @Test
    void tilgangTilEnhet_kontorsperreEnhetErNull_returnererTrue() {

        assertThat(filter.tilgangTilEnhet(HenvendelseData.builder().kontorsperreEnhetId(null).build())).isTrue();
        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId(null).build())).isTrue();

    }

    @Test
    void tilgangTilEnhet_kontorsperreEnhetErTom_returnererTrue() {

        assertThat(filter.tilgangTilEnhet(HenvendelseData.builder().kontorsperreEnhetId("").build())).isTrue();
        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId("").build())).isTrue();

    }

    @Test
    void tilgangTilEnhet_veilederHarTilgangIABAC_skalReturnereTrue() {
        EnhetId enhet = EnhetId.of("enhet");
        NavIdent veileder = NavIdent.of("veileder");

        when(auth.erEksternBruker()).thenReturn(false);
        when(auth.getLoggedInnUser()).thenReturn(veileder);
        when(auth.harTilgangTilEnhet(enhet))
                .thenReturn(true);

        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId(enhet.get()).build())).isTrue();

    }

    @Test
    void tilgangTilEnhet_veilederHarIkkeTilgangIABAC_skalReturnereFalse() {
        EnhetId enhet = EnhetId.of("enhet");
        NavIdent veileder = NavIdent.of("veileder");

        when(auth.erEksternBruker()).thenReturn(false);
        when(auth.getLoggedInnUser()).thenReturn(veileder);
        when(auth.harTilgangTilEnhet(enhet))
                .thenReturn(false);
        assertThat(filter.tilgangTilEnhet(DialogData.builder().kontorsperreEnhetId(enhet.get()).build())).isFalse();

    }

}
