package no.nav.fo.veilarbdialog.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.sts.SystemUserTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.UrlUtils.createServiceUrl;

@Configuration
public class AktorClientConfig {

    @Bean
    @Profile("!local")
    public String pdlUrl() {
        return createServiceUrl("pdl-api", "pdl", false);
    }

    @Bean
    public AktorOppslagClient aktorOppslagClient(String pdlUrl, SystemUserTokenProvider systemUserTokenProvider) {
        AktorOppslagClient aktorOppslagClient = new PdlAktorOppslagClient(
                pdlUrl,
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken
        );

        return new CachedAktorOppslagClient(aktorOppslagClient);
    }
}