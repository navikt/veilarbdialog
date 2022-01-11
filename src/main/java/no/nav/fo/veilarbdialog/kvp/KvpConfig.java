package no.nav.fo.veilarbdialog.kvp;

import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.SystemUserTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KvpConfig {

    @Value("${application.kvp.url}")
    private String kvpServiceUrl;

    @Bean
    KvpService kvpService(SystemUserTokenProvider systemUserTokenProvider) {
        return new KvpService(
                kvpServiceUrl,
                RestClient.baseClient()
        );
    }

}
