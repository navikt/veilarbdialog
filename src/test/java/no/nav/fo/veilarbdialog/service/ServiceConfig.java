package no.nav.fo.veilarbdialog.service;

import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.featuretoggle.UnleashClientImpl;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class ServiceConfig {

    @MockBean
    SystemUserTokenProvider systemUserTokenProvider;

    private final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);

    @Bean
    SystemUserTokenProvider systemUserTokenProvider() {
        return new MockedSystemUserTokenProvider();
    }

    private static class MockedSystemUserTokenProvider implements SystemUserTokenProvider {

        @Override
        public String getSystemUserToken() {
            return "test-token";
        }

    }

    @Bean
    AktorOppslagClient aktorOppslagClient() {
        when(aktorOppslagClient.hentAktorId(Fnr.of("112233456789")))
                .thenReturn(AktorId.of("12345"));
        when(aktorOppslagClient.hentFnr(AktorId.of("12345")))
                .thenReturn(Fnr.of("112233456789"));
        return aktorOppslagClient;
    }

    @Bean
    Unleash unleash() {
        return new FakeUnleash();
    }

    @Bean
    UnleashClient unleashClient(Unleash unleash) {
        return new UnleashClientImpl(unleash);
    }

}
