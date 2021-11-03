package no.nav.fo.veilarbdialog.config.kafka.onprem;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.service.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.util.Map;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;

@Profile("!local")
@Configuration
@EnableConfigurationProperties({KafkaOnpremProperties.class})
public class KafkaOnpremConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbdialog-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbdialog-producer";

    @Autowired
    KafkaConsumerClient<String, String> consumerClient;

    @Bean
    public Map<String, TopicConsumer<String, String>> topicConsumers(
            KafkaConsumerService kafkaConsumerService,
            KafkaOnpremProperties kafkaProperties
    ) {
        return Map.of(
                kafkaProperties.oppfolgingAvsluttetTopic,
                jsonConsumer(OppfolgingAvsluttetKafkaDTO.class, kafkaConsumerService::behandleOppfolgingAvsluttet),

                kafkaProperties.kvpAvsluttetTopic,
                jsonConsumer(KvpAvsluttetKafkaDTO.class, kafkaConsumerService::behandleKvpAvsluttet)
        );
    }

    @Bean
    public KafkaConsumerClient<String, String> consumerClient(
            Map<String, TopicConsumer<String, String>> topicConsumers,
            Credentials credentials,
            KafkaOnpremProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        return KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withConsumers(topicConsumers)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();
    }

    @Bean
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
