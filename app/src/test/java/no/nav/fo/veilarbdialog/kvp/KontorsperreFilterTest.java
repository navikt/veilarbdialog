package no.nav.fo.veilarbdialog.kvp;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import no.nav.apiapp.security.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

public class KontorsperreFilterTest {

    private static final String ENHET = "123";
    private KontorsperreFilter filter = new KontorsperreFilter();
    private PepClient pepClient = mock(PepClient.class);
    
    @Before
    public void setUp() {
        pepClient = mock(PepClient.class);
        filter.pepClient = pepClient;
    }

    @Test
    public void skal_ha_tilgang_hvis_kontor_er_blank_eller_null() throws PepException {
        assertThat(filter.harTilgang(null), is(true));
        assertThat(filter.harTilgang(""), is(true));
    }
    
    @Test
    public void skal_ha_tilgang_hvis_abac_sier_ja() throws PepException {
        when(pepClient.harTilgangTilEnhet(ENHET)).thenReturn(true);
        assertThat(filter.harTilgang(ENHET), is(true));
    }
    
    @Test
    public void skal_ikke_ha_tilgang_hvis_abac_sier_nei() throws PepException {
        when(pepClient.harTilgangTilEnhet(ENHET)).thenReturn(false);
        assertThat(filter.harTilgang(ENHET), is(false));
        verify(pepClient).harTilgangTilEnhet(ENHET);
    }
}
