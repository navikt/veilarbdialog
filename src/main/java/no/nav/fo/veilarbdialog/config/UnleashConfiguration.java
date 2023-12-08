package no.nav.fo.veilarbdialog.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Configuration
public class UnleashConfiguration {

    @Value("${unleash.appName}")
    private String appName;

    @Value("${unleash.url}")
    private String url;

    @Value("${unleash.token}")
    private String token;

    @Value("${unleash.instanceId}")
    private String instanceId;


    @Bean
    public Unleash unleash() {
        return new DefaultUnleash(toUnleashConfig());
    }

    private UnleashConfig toUnleashConfig() {
        var environment = isProduction().orElse(false) ? "production" : "development";

        return UnleashConfig.builder()
                .appName(appName)
                .instanceId(instanceId)
                .unleashAPI(url + "/api")
                .apiKey(token)
                .environment(environment)
                .build();
    }
}
