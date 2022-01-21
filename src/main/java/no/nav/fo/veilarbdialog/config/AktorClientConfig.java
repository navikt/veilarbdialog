package no.nav.fo.veilarbdialog.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.AktorregisterHttpClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.sts.SystemUserTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AktorClientConfig {
    @Value("${application.aktorRegister.url}")
    private String aktorRegisterUrl;
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public AktorOppslagClient aktorOppslagClient(SystemUserTokenProvider tokenProvider) {
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
            aktorRegisterUrl, applicationName, tokenProvider::getSystemUserToken
        );

        return new CachedAktorOppslagClient(aktorregisterClient);
    }
}