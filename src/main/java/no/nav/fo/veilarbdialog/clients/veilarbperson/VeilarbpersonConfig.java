package no.nav.fo.veilarbdialog.clients.veilarbperson;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VeilarbpersonConfig {

    @Value("${VEILARBPERSON_URL}")
    private String baseUrl;

    @Bean
    public VeilarbpersonClient veilarbpersonClient(OkHttpClient client) {
        return new VeilarbpersonClientImpl(baseUrl, client);
    }

}
