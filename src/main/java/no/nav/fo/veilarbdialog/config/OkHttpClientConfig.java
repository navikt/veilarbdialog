package no.nav.fo.veilarbdialog.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;


@Configuration
public class OkHttpClientConfig {

    @Bean OkHttpClient veilarbpersonClient(MeterRegistry meterRegistry, Interceptor veilarbpersonInterceptor) {
        return RestClient.baseClientBuilder()
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .addInterceptor(veilarbpersonInterceptor).build();
    }

    @Bean OkHttpClient veilarbOppfolgingClient(MeterRegistry meterRegistry, Interceptor oppfolgingInterceptor) {
        return RestClient.baseClientBuilder()
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .addInterceptor(oppfolgingInterceptor).build();
    }

    @Value("${application.veilarbperson.api.scope}") String veilarbpersonScope;
    @Value("${application.veilarboppfolging.api.azureScope}") String veilarboppfolgingapiScope;


    @Bean
    Interceptor oppfolgingInterceptor(AuthService auth,
                  AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient,
                  AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        Supplier<String> tokenProvider = () -> {
            if (auth.erInternBruker()) {
                return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(veilarboppfolgingapiScope, auth.getInnloggetBrukerToken());
            } else {
                return azureAdMachineToMachineTokenClient.createMachineToMachineToken(veilarboppfolgingapiScope);
            }
        };
        return chain -> {
            var original = chain.request();
            var newReq = original
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + tokenProvider)
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        };
    }
    @Bean
    Interceptor veilarbpersonInterceptor(AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        return chain -> {
            var original = chain.request();
            var newReq = original
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + azureAdMachineToMachineTokenClient.createMachineToMachineToken(veilarbpersonScope))
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        };
    }
}
