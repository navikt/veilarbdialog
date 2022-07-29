package no.nav.fo.veilarbdialog.config;

import no.nav.common.sts.SystemUserTokenProvider;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.mockito.Mockito.mock;

@Configuration
public class ApplicationTestConfig {
    @Bean
    public String pdlUrl(Environment environment) {
        return environment.getRequiredProperty("application.pdl.api.url");
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider() {
        SystemUserTokenProvider systemUserTokenProvider = mock(SystemUserTokenProvider.class);
        Mockito.when(systemUserTokenProvider.getSystemUserToken()).thenReturn("mockSystemUserToken");
        return systemUserTokenProvider;
    }

}
