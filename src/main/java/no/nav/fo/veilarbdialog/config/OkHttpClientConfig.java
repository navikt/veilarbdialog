package no.nav.fo.veilarbdialog.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.fo.veilarbdialog.clients.util.TokenProvider;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean OkHttpClient dialogvarslerClient(MeterRegistry meterRegistry, Interceptor pleaseAuthInterceptor) {
        return RestClient.baseClientBuilder()
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests").build())
                .addInterceptor(pleaseAuthInterceptor).build();
    }

    @Value("${application.veilarbperson.api.scope}") String veilarbpersonScope;



    @Bean
    Interceptor oppfolgingAuthInterceptor(TokenProvider veilarbOppfolgingTokenProvider,
                                          @Value("${application.veilarboppfolging.api.azureScope}") String veilarboppfolgingAzureScope,
                                          @Value("${application.veilarboppfolging.api.tokenXScope}") String veilarboppfolgingTokenXScope) {
        return chain -> {
            var original = chain.request();
            var newReq = original
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + veilarbOppfolgingTokenProvider.get(veilarboppfolgingAzureScope, veilarboppfolgingTokenXScope))
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

    @Bean
    Interceptor pleaseAuthInterceptor(TokenProvider tokenProvider, @Value("${application.please.api.azureScope}") String pleaseAzureScope,  @Value("${application.please.api.tokenXScope}") String pleaseTokenXScope) {
        return chain -> {
            var original = chain.request();
            var token = tokenProvider.get(pleaseAzureScope, pleaseTokenXScope);
            var newReq = original
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(newReq);
        };
    }
}
