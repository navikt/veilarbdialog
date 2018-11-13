package no.nav.fo.veilarbdialog.config;

import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

import static no.nav.fo.veilarbdialog.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;

@Configuration
public class KvpClientConfig {

    private String VEILARBOPPFOLGINGAPI_URL = EnvironmentUtils.getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY);

    @Value("${kvp.connect.timeout.ms:1000}")
    private int KVP_CONNECT_TIMEOUT;

    @Value("${kvp.read.timeout.ms:1000}")
    private int KVP_READ_TIMEOUT;

    @Bean
    public KvpClient kvpClient() {
        RestUtils.RestConfig.RestConfigBuilder configBuilder = RestUtils.RestConfig.builder();

        configBuilder.readTimeout(KVP_READ_TIMEOUT);
        configBuilder.connectTimeout(KVP_CONNECT_TIMEOUT);

        RestUtils.RestConfig config = configBuilder.build();
        Client client = RestUtils.createClient(config);

        return new KvpClient(VEILARBOPPFOLGINGAPI_URL, client);
    }
}
