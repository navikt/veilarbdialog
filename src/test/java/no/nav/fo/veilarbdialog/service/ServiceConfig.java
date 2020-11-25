package no.nav.fo.veilarbdialog.service;

import no.finn.unleash.strategy.Strategy;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.IdentOppslag;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.sts.SystemUserTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
        when(systemUserTokenProvider.getSystemUserToken())
                .thenReturn("test-token");
        return systemUserTokenProvider;
    }

    @Bean
    AktorregisterClient aktorregisterClient() {
        when(aktorregisterClient.hentAktorId(anyString()))
                .thenReturn(null);
        when(aktorregisterClient.hentAktorId(anyList()))
                .thenReturn(null);
        when(aktorregisterClient.hentFnr(anyString()))
                .thenReturn(null);
        when(aktorregisterClient.hentFnr(anyList()))
                .thenReturn(null);
        return aktorregisterClient;
    }

    @Bean
    UnleashService unleashService() {
        when(unleashService.checkHealth())
                .thenReturn(HealthCheckResult.healthy());
        when(unleashService.isEnabled(anyString()))
                .thenReturn(false);
        when(unleashService.isEnabled(anyString(), any()))
                .thenReturn(false);
        return unleashService;
    }

}
