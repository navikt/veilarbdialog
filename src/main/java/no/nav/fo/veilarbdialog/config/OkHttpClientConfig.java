package no.nav.fo.veilarbdialog.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.SystemUserTokenProvider;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OkHttpClientConfig {

    @Bean
    public OkHttpClient client(SystemUserTokenProvider tokenProvider, MeterRegistry meterRegistry) {
        var builder = RestClient.baseClientBuilder();
        builder.eventListener(OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests")
                .build());
        return builder.build();
    }
}
