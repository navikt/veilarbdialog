package no.nav.fo.veilarbdialog.service;

import lombok.Getter;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.AktorregisterHttpClient;
import no.nav.common.client.aktorregister.CachedAktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.featuretoggle.UnleashServiceConfig;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

import static lombok.AccessLevel.PACKAGE;

@Configuration
@Getter(PACKAGE)
public class ServiceConfig {

    @Value("${application.dialog.url}")
    private URL arbeidsrettetDialogUrl;

    @Value("${application.sts.discovery.url}")
    private URL discoveryUrl;

    @Value("${application.aktorregister.url}")
    private String aktorregisterUrl;

    @Value("${application.unleash.url}")
    private String unleashUrl;

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    SystemUserTokenProvider systemUserTokenProvider(Credentials serviceUser) {
        return new NaisSystemUserTokenProvider(discoveryUrl.toString(), serviceUser.username, serviceUser.password);
    }

    @Bean
    AktorregisterClient aktorregisterClient(SystemUserTokenProvider tokenProvider) {
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
                aktorregisterUrl,
                applicationName,
                tokenProvider::getSystemUserToken
        );
        return new CachedAktorregisterClient(aktorregisterClient);
    }

    @Bean
    UnleashService unleashService() {
        UnleashServiceConfig config = UnleashServiceConfig
                .builder()
                .applicationName(applicationName)
                .unleashApiUrl(unleashUrl)
                .build();
        return new UnleashService(config);
    }

}
