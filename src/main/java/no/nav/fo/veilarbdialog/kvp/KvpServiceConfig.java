package no.nav.fo.veilarbdialog.kvp;

import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
public class KvpServiceConfig {

    @Value("${application.veilarboppfolging.api.url}")
    private String baseUrl;

    @Bean
    public KvpService kvpService(AzureAdMachineToMachineTokenClient tokenClient) {
        String tokenScope = String.format(
                "api://%s-fss.pto.veilarboppfolging/.default",
                isProduction().orElse(false) ? "prod" : "dev"
        );

        OkHttpClient okHttpClient = RestClient.baseClient();

        return new KvpService(baseUrl, okHttpClient, () -> tokenClient.createMachineToMachineToken(tokenScope));
    }
}
