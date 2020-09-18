package no.nav.fo.veilarbdialog;

import lombok.Getter;
import lombok.Setter;
import no.nav.common.utils.SslUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
public class Application {

    public static void main(String... args) {
        SslUtils.setupTruststore();
        SpringApplication.run(Application.class, args);
    }

}
