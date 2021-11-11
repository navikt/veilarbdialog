package no.nav.fo.veilarbdialog.service;

import lombok.Getter;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.AktorregisterHttpClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.featuretoggle.UnleashClientImpl;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static lombok.AccessLevel.PACKAGE;

@Configuration
@Getter(PACKAGE)
public class ServiceConfig {

    @Value("${application.dialog.url}")
    private String arbeidsrettetDialogUrl;

    @Value("${application.sts.discovery.url}")
    private String discoveryUrl;

    @Value("${application.aktorregister.url}")
    private String aktorregisterUrl;

    @Value("${application.unleash.url}")
    private String unleashUrl;

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    SystemUserTokenProvider systemUserTokenProvider(Credentials systemUser) {
        return new NaisSystemUserTokenProvider(discoveryUrl, systemUser.username, systemUser.password);
    }

    @Bean
    AktorOppslagClient aktoroppslagClient(SystemUserTokenProvider tokenProvider) {
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
                aktorregisterUrl,
                applicationName,
                tokenProvider::getSystemUserToken
        );
        return new CachedAktorOppslagClient(aktorregisterClient);
    }

    @Bean
    UnleashClient unleashService() {
        return new UnleashClientImpl(unleashUrl, applicationName) {
        };
    }

}
