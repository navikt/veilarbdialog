package no.nav.fo.veilarbdialog.config.kafka.onprem;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.fo.veilarbdialog.config.KafkaOnpremProperties;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.service.KafkaConsumerService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.Map;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
@Slf4j
public class KafkaTestConfig {

    @Bean
    public KafkaOnpremProperties kafkaProperties(EmbeddedKafkaBroker embeddedKafkaBroker) {
        KafkaOnpremProperties kafkaProperties = new KafkaOnpremProperties();

        kafkaProperties.setBrokersUrl(embeddedKafkaBroker.getBrokersAsString());
        kafkaProperties.setEndringPaaDialogTopic("aapen-fo-endringPaaDialog-v1-test");
        kafkaProperties.setKvpAvsluttetTopic("aapen-arbeidsrettetOppfolging-kvpAvsluttet-v1-test");
        kafkaProperties.setOppfolgingAvsluttetTopic("aapen-arbeidsrettetOppfolging-oppfolgingAvsluttet-v1-test");

        return kafkaProperties;
    }

    @Primary
    @Bean
    EmbeddedKafkaBroker embeddedKafka() {
        return new EmbeddedKafkaBroker(1);
    }

    @Bean
    public Map<String, TopicConsumer<String, String>> topicConsumers(
            KafkaConsumerService kafkaConsumerService,
            KafkaOnpremProperties kafkaProperties
    ) {
        return Map.of(
                kafkaProperties.getOppfolgingAvsluttetTopic(),
                jsonConsumer(OppfolgingAvsluttetKafkaDTO.class, kafkaConsumerService::behandleOppfolgingAvsluttet),

                kafkaProperties.getKvpAvsluttetTopic(),
                jsonConsumer(KvpAvsluttetKafkaDTO.class, kafkaConsumerService::behandleKvpAvsluttet)
        );
    }

    @Bean
    public KafkaConsumerClient<String, String> consumerClient(
            Map<String, TopicConsumer<String, String>> topicConsumers,
            KafkaOnpremProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        return KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(kafkaConsumerProperties(kafkaProperties.getBrokersUrl()))
                .withConsumers(topicConsumers)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();
    }

    @Bean
    public KafkaProducerClient<String, String> producerClient(KafkaOnpremProperties kafkaProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(kafkaProducerProperties(kafkaProperties.getBrokersUrl()))
                .build();
    }

    private Properties kafkaConsumerProperties(String brokerUrl) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        return props;
    }

    private Properties kafkaProducerProperties(String brokerUrl) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

}
