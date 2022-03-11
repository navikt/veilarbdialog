package no.nav.fo.veilarbdialog.clients.veilarboppfolging;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VeilarboppfolgingConfig {

    @Value("${application.veilarboppfolging.api.url}")
    private String baseUrl;

    @Bean
    public VeilarboppfolgingClient veilarboppfolgingClient(OkHttpClient client) {
        return new VeilarboppfolgingClientImpl(baseUrl, client);
    }

}
