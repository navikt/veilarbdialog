package no.nav.fo.veilarbdialog.kvp;

import no.nav.common.abac.Pep;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class KontorsperreFilterTest {

    private static final String ENHET = "123";
    private KontorsperreFilter filter;
    private Pep pepClient;

    @Before
    public void setUp() {

        pepClient = mock(Pep.class);
        filter = new KontorsperreFilter(pepClient);

    }

    @Test
    public void skal_ha_tilgang_hvis_kontor_er_blank_eller_null() {

        assertThat(filter.harTilgang(null, null)).isTrue();
        assertThat(filter.harTilgang("", "")).isTrue();

    }

    @Test
    public void skal_ha_tilgang_hvis_abac_sier_ja() {

        when(pepClient.harVeilederTilgangTilEnhet("veileder", "enhet"))
                .thenReturn(true);
        assertThat(filter.harTilgang("veileder", "enhet")).isTrue();
        verify(pepClient).harVeilederTilgangTilEnhet("veileder", "enhet");

    }

    @Test
    public void skal_ikke_ha_tilgang_hvis_abac_sier_nei() {

        when(pepClient.harVeilederTilgangTilEnhet("veileder", "enhet"))
                .thenReturn(false);
        assertThat(filter.harTilgang("veileder", "enhet")).isFalse();
        verify(pepClient).harVeilederTilgangTilEnhet("veileder", "enhet");

    }
}
