package no.nav.fo.veilarbdialog.config.kafka.onprem;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Properties;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;

@Configuration
@EnableConfigurationProperties({KafkaOnpremProperties.class})
public class KafkaOnpremConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbdialog-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbdialog-producer";

    private static final String ONPREM_KAFKA_DISABLED = "veilarbaktivitet.kafka.onprem.consumer.disabled";

    @Bean
    public KafkaConsumerClient consumerClient(
            List<TopicConsumerConfig<?, ?>> topicConfigs,
            MeterRegistry meterRegistry,
            Properties onPremConsumerProperties,
            UnleashClient unleashClient
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremConsumerProperties)
                .withToggle(() -> unleashClient.isEnabled(ONPREM_KAFKA_DISABLED));

        topicConfigs.forEach(it -> {
            clientBuilder.withTopicConfig(new KafkaConsumerClientBuilder.TopicConfig().withConsumerConfig(it).withMetrics(meterRegistry).withLogging());
        });

        var client = clientBuilder.build();

        client.start();

        return client;
    }

    @Bean
    public KafkaProducerClient<String, String> producerClient(Properties onPremProducerProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(onPremProducerProperties)
                .build();
    }

    @Bean
    @Profile("!dev")
    Properties onPremProducerProperties(KafkaOnpremProperties kafkaOnpremProperties, Credentials credentials) {
        return onPremDefaultProducerProperties(PRODUCER_CLIENT_ID, kafkaOnpremProperties.brokersUrl, credentials);
    }

    @Bean
    @Profile("!dev")
    Properties onPremConsumerProperties(KafkaOnpremProperties kafkaOnpremProperties, Credentials credentials) {
        return onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaOnpremProperties.getBrokersUrl(), credentials);
    }

}
