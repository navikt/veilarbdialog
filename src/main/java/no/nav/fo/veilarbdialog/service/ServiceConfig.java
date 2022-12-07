package no.nav.fo.veilarbdialog.service;

import lombok.Getter;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static lombok.AccessLevel.PACKAGE;

@Configuration
@Getter(PACKAGE)
public class ServiceConfig {

    @Value("${application.dialog.url}")
    private String arbeidsrettetDialogUrl;

    @Value("${application.sts.discovery.url}")
    private String discoveryUrl;

    @Value("${application.unleash.url}")
    private String unleashUrl;

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    @Profile("!local")
    SystemUserTokenProvider systemUserTokenProvider(Credentials systemUser) {
        return new NaisSystemUserTokenProvider(discoveryUrl, systemUser.username, systemUser.password);
    }

    @Bean
    @Profile("!local")
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildMachineToMachineTokenClient();
    }

}
