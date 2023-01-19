package no.nav.fo.veilarbdialog.clients.util;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientWrapperConfig {
    @Bean
    public HttpClientWrapper veilarboppfolgingClientWrapper(@Value("${application.veilarboppfolging.api.url}") String baseUrl, OkHttpClient veilarbOppfolgingClient) {
        return new HttpClientWrapper(
                veilarbOppfolgingClient,
                baseUrl
        );
    }
    @Bean
    public HttpClientWrapper veilarbpersonClientWrapper(@Value("${application.veilarbperson.api.url}") String baseUrl, OkHttpClient veilarbpersonClient) {
        return new HttpClientWrapper(
                veilarbpersonClient,
                baseUrl
        );
    }
}
