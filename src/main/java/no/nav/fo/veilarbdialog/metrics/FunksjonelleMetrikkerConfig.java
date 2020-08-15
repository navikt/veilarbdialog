package no.nav.fo.veilarbdialog.metrics;

import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.SensuConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@RequiredArgsConstructor
public class FunksjonelleMetrikkerConfig {

    @Value("spring.application.name")
    private String applicationName;

    @Value("application.cluster")
    private String cluster;

    @Value("application.namespace")
    private String namespace;

    @Bean
    FunksjonelleMetrikker funksjonelleMetrikker()
            throws UnknownHostException {

        return new FunksjonelleMetrikker(
                new InfluxClient(
                        SensuConfig
                                .builder()
                                .sensuHost("sensu.nais")
                                .sensuPort(3030)
                                .application(applicationName)
                                .hostname(InetAddress.getLocalHost().getCanonicalHostName())
                                .cluster(cluster)
                                .namespace(namespace)
                                .retryInterval(5000L)
                                .connectTimeout(3000)
                                .queueSize(20000)
                                .maxBatchTime(10000L)
                                .batchSize(500)
                                .cleanupOnShutdown(true)
                                .build()
                )
        );

    }

}
