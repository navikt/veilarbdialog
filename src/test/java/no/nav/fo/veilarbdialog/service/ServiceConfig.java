package no.nav.fo.veilarbdialog.service;

import no.finn.unleash.UnleashContext;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.IdentOppslag;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.featuretoggle.UnleashServiceConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.fo.veilarbdialog.TestApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

@TestConfiguration
public class ServiceConfig {

    @MockBean
    SystemUserTokenProvider systemUserTokenProvider;

    @MockBean
    AktorregisterClient aktorregisterClient;

    @MockBean
    UnleashService unleashService;

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
    AktorregisterClient aktorregisterClient() {
        return new MockedAktorregisterClient();
    }

    private static class MockedAktorregisterClient implements AktorregisterClient {

        @Override
        public String hentFnr(String aktorId) {
            return null;
        }

        @Override
        public String hentAktorId(String fnr) {
            return null;
        }

        @Override
        public List<IdentOppslag> hentFnr(List<String> aktorIdListe) {
            return null;
        }

        @Override
        public List<IdentOppslag> hentAktorId(List<String> fnrListe) {
            return null;
        }

        @Override
        public HealthCheckResult checkHealth() {
            return HealthCheckResult.healthy();
        }

    }

    @Bean
    UnleashService unleashService() {
        return new MockedUnleashService();
    }

    private static class MockedUnleashService extends UnleashService {

        private MockedUnleashService() {
            super(
                    UnleashServiceConfig
                            .builder()
                            .unleashApiUrl("http://not.used/but/must/be/parseable")
                            .applicationName(TestApplication.class.getSimpleName())
                            .build()
            );
        }

        @Override
        public boolean isEnabled(String toggleName) {
            return false;
        }

        @Override
        public boolean isEnabled(String toggleName, UnleashContext unleashContext) {
            return false;
        }

        @Override
        public HealthCheckResult checkHealth() {
            return HealthCheckResult.healthy();
        }

    }

}
