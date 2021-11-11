package no.nav.fo.veilarbdialog.config.kafka.onprem;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.util.List;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;

@Profile("!local")
@Configuration
@EnableConfigurationProperties({KafkaOnpremProperties.class})
public class KafkaOnpremConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbdialog-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbdialog-producer";

    @Autowired
    KafkaConsumerClient consumerClient;

    @Bean
    @Profile("!dev")
    public KafkaConsumerClient consumerClient(
            List<TopicConsumerConfig<?,?>> topicConfigs,
            Credentials credentials,
            KafkaOnpremProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        KafkaConsumerClientBuilder clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials));

        topicConfigs.forEach(it ->
            clientBuilder.withTopicConfig(
                    new KafkaConsumerClientBuilder
                            .TopicConfig()
                            .withConsumerConfig(it)
                            .withMetrics(meterRegistry)
                            .withLogging()
            )
        );
        var client = clientBuilder.build();
        client.start();

        return client;
    }

    @Bean
    @Profile("!dev")
    public KafkaProducerClient<String, String> producerClient(KafkaOnpremProperties kafkaProperties, Credentials credentials, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(onPremDefaultProducerProperties(PRODUCER_CLIENT_ID, kafkaProperties.getBrokersUrl(), credentials))
                .build();
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
    }

}
