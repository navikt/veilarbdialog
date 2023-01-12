package no.nav.fo.veilarbdialog.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.VeilarbOppfolgingTokenProvider;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;


@Configuration
public class OkHttpClientConfig {

    @Bean OkHttpClient veilarbpersonClient(MeterRegistry meterRegistry, Interceptor veilarbpersonAuthInterceptor) {
        return RestClient.baseClientBuilder()
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .addInterceptor(veilarbpersonAuthInterceptor).build();
    }

    @Bean OkHttpClient veilarbOppfolgingClient(MeterRegistry meterRegistry, Interceptor oppfolgingAuthInterceptor) {
        return RestClient.baseClientBuilder()
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .addInterceptor(oppfolgingAuthInterceptor).build();
    }

    @Value("${application.veilarbperson.api.scope}") String veilarbpersonScope;
    @Value("${application.veilarboppfolging.api.azureScope}") String veilarboppfolgingapiScope;
    @Value("${application.veilarboppfolging.api.tokenXScope}") String veilarboppfolgingapiScopeTokenX;


    @Bean
    Interceptor oppfolgingAuthInterceptor(VeilarbOppfolgingTokenProvider veilarbOppfolgingTokenProvider) {
        return chain -> {
            var original = chain.request();
            var newReq = original
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + veilarbOppfolgingTokenProvider.get())
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        };
    }
    @Bean
    Interceptor veilarbpersonAuthInterceptor(AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
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
