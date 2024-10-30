package no.nav.fo.veilarbdialog.service;

import no.nav.veilarbdialog.eventsLogger.BigQueryClient;
import no.nav.veilarbdialog.eventsLogger.BigQueryClientImplementation;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.builder.TokenXTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ServiceConfig {

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

    @Bean
    @Profile("!local")
    public BigQueryClient bigQueryClient(BigQueryClientImplementation bigQueryClientImplementation) {
        return bigQueryClientImplementation;
    }

}
