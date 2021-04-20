package no.nav.fo.veilarbdialog.auth;

import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.types.identer.NavIdent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class AuthConfig {

    private final Pep pep = mock(Pep.class);
    private final AuthContextHolderThreadLocal authContextHolderThreadLocal = mock(AuthContextHolderThreadLocal.class);

    @PostConstruct
    public void postConstruct() {
        when(pep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(pep.harTilgangTilEnhet(any(), any())).thenReturn(true);
        when(pep.harVeilederTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(pep.harTilgangTilPerson(any(), any(), any())).thenReturn(true);

        when(authContextHolderThreadLocal.getIdTokenString()).thenReturn(Optional.of("ID_TOKEN_STRING"));
        when(authContextHolderThreadLocal.getSubject()).thenReturn(Optional.of("SUBJECT"));
        when(authContextHolderThreadLocal.getNavIdent()).thenReturn(Optional.of(NavIdent.of("NAVIDENT")));

        // TODO: Note these. Switch depending on which usage you are testing.
        when(authContextHolderThreadLocal.erInternBruker()).thenReturn(true);
        when(authContextHolderThreadLocal.erEksternBruker()).thenReturn(false);
    }

    @Primary
    @Bean
    public Pep pep() {
        return pep;
    }

    @Primary
    @Bean
    public AuthContextHolder authContextHolderThreadLocal() {
        return authContextHolderThreadLocal;
    }

}
