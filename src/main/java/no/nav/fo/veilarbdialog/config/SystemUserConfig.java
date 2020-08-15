package no.nav.fo.veilarbdialog.config;

import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemUserConfig {

    @Value("${application.serviceuser.username}")
    private String serviceUsername;

    @Value("${application.serviceuser.password}")
    private String servicePassword;

    @Bean
    Credentials systemUser() {
        return new Credentials(serviceUsername, servicePassword);
    }

}
