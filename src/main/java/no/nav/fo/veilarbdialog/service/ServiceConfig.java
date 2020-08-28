package no.nav.fo.veilarbdialog.service;

import lombok.Getter;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.AktorregisterHttpClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.featuretoggle.UnleashServiceConfig;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

import static lombok.AccessLevel.PACKAGE;

@Configuration
@Getter(PACKAGE)
public class ServiceConfig {

    @Value("no.nav.arbeidsrettetDialogUrl")
    private URL arbeidsrettetDialogUrl;

    @Value("no.nav.sts.discoveryUrl")
    private URL discoveryUrl;

    @Value("no.nav.sts.srvUsername")
    private String srvUsername;

    @Value("no.nav.sts.srvPassword")
    private String srvPassword;

    @Bean
    AktorregisterClient aktorregisterClient() {
        return new AktorregisterHttpClient("", "", null); // TODO: Configure.
    }

    @Bean
    SystemUserTokenProvider systemUserTokenProvider() {
        return new NaisSystemUserTokenProvider(discoveryUrl.toString(), srvUsername, srvPassword);
    }

    @Bean
    UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.resolveFromEnvironment());
    }

}
