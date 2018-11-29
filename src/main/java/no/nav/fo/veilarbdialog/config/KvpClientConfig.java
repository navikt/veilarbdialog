package no.nav.fo.veilarbdialog.config;

import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

import static no.nav.fo.veilarbdialog.config.ApplicationConfig.VEILARBOPPFOLGINGAPI_URL_PROPERTY;

@Configuration
public class KvpClientConfig {

    private String VEILARBOPPFOLGINGAPI_URL = EnvironmentUtils.getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY);

    @Bean
    public KvpClient kvpClient() {
        Client client = RestUtils.createClient();
        return new KvpClient(VEILARBOPPFOLGINGAPI_URL, client);
    }
}
