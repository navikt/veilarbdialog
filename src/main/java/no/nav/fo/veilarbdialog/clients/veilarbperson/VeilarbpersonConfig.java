package no.nav.fo.veilarbdialog.clients.veilarbperson;

import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
public class VeilarbpersonConfig {

    @Value("${application.veilarbperson.api.url}")
    private String baseUrl;

    @Bean
    public VeilarbpersonClient veilarbpersonClient(AzureAdMachineToMachineTokenClient tokenClient) {
        String tokenScop = String.format("api://%s-fss.pto.veilarbperson/.default",
                isProduction().orElse(false) ? "prod" : "dev"
        );
        return new VeilarbpersonClientImpl(baseUrl, () -> tokenClient.createMachineToMachineToken(tokenScop));
    }

}
