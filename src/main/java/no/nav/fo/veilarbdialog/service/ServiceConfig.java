package no.nav.fo.veilarbdialog.service;

import lombok.Getter;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.builder.TokenXTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
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

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    @Profile("!local")
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildMachineToMachineTokenClient();
    }

    @Bean
    @Profile("!local")
    public TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient() {
        return TokenXTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
    }

    @Bean
    @Profile("!local")
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
    }

}
