package no.nav.fo.veilarbdialog.clients.veilarboppfolging;

import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
public class VeilarboppfolgingConfig {

    @Value("${application.veilarboppfolging.api.url}")
    private String baseUrl;
    @Value("${application.veilarboppfolging.api.scope}")
    private String veilarboppfolgingapi_scope;

    @Bean
    public VeilarboppfolgingClient veilarboppfolgingClient(AzureAdMachineToMachineTokenClient tokenClient) {
        OkHttpClient okHttpClient = RestClient.baseClient();

        return new VeilarboppfolgingClientImpl(baseUrl, okHttpClient, () -> tokenClient.createMachineToMachineToken(veilarboppfolgingapi_scope));
    }

}
