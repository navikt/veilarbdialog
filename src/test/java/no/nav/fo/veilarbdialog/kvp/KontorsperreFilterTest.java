package no.nav.fo.veilarbdialog.kvp;

import no.nav.fo.veilarbdialog.auth.AuthService;
import org.junit.Before;
import org.junit.Test;

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
    public void skal_ha_tilgang_hvis_kontor_er_blank_eller_null() {

        assertThat(filter.harTilgang(null, null)).isTrue();
        assertThat(filter.harTilgang("", "")).isTrue();

    }

    @Test
    public void skal_ha_tilgang_hvis_abac_sier_ja() {

        when(auth.harVeilederTilgangTilEnhet("veileder", "enhet"))
                .thenReturn(true);
        assertThat(filter.harTilgang("veileder", "enhet")).isTrue();
        verify(auth).harVeilederTilgangTilEnhet("veileder", "enhet");

    }

    @Test
    public void skal_ikke_ha_tilgang_hvis_abac_sier_nei() {

        when(auth.harVeilederTilgangTilEnhet("veileder", "enhet"))
                .thenReturn(false);
        assertThat(filter.harTilgang("veileder", "enhet")).isFalse();
        verify(auth).harVeilederTilgangTilEnhet("veileder", "enhet");

    }

}
