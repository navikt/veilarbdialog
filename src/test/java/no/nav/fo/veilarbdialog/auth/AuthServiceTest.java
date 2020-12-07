package no.nav.fo.veilarbdialog.auth;

import no.nav.common.abac.Pep;
import no.nav.fo.veilarbdialog.domain.TestKontorsperreEnhetData;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private final Pep pep = mock(Pep.class);
    private final AuthService auth = spy(new AuthService(pep));

    @Test
    public void skal_ha_tilgang_hvis_kontor_er_blank_eller_null() {
        assertThat(auth.filterKontorsperre(new TestKontorsperreEnhetData(null))).isTrue();
        assertThat(auth.filterKontorsperre(new TestKontorsperreEnhetData(""))).isTrue();
    }

    @Test
    public void skal_ha_tilgang_hvis_abac_sier_ja() {

        when(auth.getIdent())
                .thenReturn(Optional.of("veileder"));
        when(pep.harVeilederTilgangTilEnhet("veileder", "enhet"))
                .thenReturn(true);
        assertThat(auth.filterKontorsperre(new TestKontorsperreEnhetData("enhet"))).isTrue();
        verify(pep).harVeilederTilgangTilEnhet("veileder", "enhet");

    }

    @Test
    public void skal_ikke_ha_tilgang_hvis_abac_sier_nei() {

        when(auth.getIdent())
                .thenReturn(Optional.of("veileder"));
        when(pep.harVeilederTilgangTilEnhet("veileder", "enhet"))
                .thenReturn(false);
        assertThat(auth.filterKontorsperre(new TestKontorsperreEnhetData("enhet"))).isFalse();
        verify(pep).harVeilederTilgangTilEnhet("veileder", "enhet");

    }

}