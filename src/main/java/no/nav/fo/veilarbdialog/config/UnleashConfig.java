package no.nav.fo.veilarbdialog.config;

import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnleashConfig {

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.resolveFromEnvironment());
    }

}
