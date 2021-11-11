package no.nav.fo.veilarbdialog.config.kafka.onprem;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.common.utils.Credentials;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.List;
import java.util.Properties;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.fo.veilarbdialog.config.kafka.onprem.KafkaOnpremConfig.CONSUMER_GROUP_ID;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
@Slf4j
public class KafkaOnpremTestConfig {

    @Bean
    @Primary
    public KafkaOnpremProperties kafkaOnpremProperties(EmbeddedKafkaBroker embeddedKafkaBroker, KafkaOnpremProperties kafkaOnpremProperties) {
        kafkaOnpremProperties.setBrokersUrl(embeddedKafkaBroker.getBrokersAsString());

        return kafkaOnpremProperties;
    }


    @Bean
    public KafkaProducerClient<String, String> producerClient(KafkaOnpremProperties kafkaOnpremProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(kafkaProducerProperties(kafkaOnpremProperties.getBrokersUrl()))
                .build();
    }

    @Bean
    public KafkaConsumerClient consumerClient(
            List<TopicConsumerConfig<?,?>> topicConfigs,
            KafkaOnpremProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        Properties properties = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withBrokerUrl(kafkaProperties.getBrokersUrl())
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .build();

        KafkaConsumerClientBuilder clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(properties);

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

    private Properties kafkaProducerProperties(String brokerUrl) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

}
