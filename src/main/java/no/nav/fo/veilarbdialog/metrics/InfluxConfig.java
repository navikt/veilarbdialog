package no.nav.fo.veilarbdialog.metrics;

import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.metrics.SensuConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class InfluxConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.cluster}")
    private String cluster;

    @Value("${application.namespace}")
    private String namespace;

    @Bean
    MetricsClient metricsClient() throws UnknownHostException {
        return new MetricsClient() {
            @Override
            public void report(Event event) {}
            @Override
            public void report(String s, Map<String, Object> map, Map<String, String> map1, long l) {}
        };
    }
}
