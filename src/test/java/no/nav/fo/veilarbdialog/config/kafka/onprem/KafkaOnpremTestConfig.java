package no.nav.fo.veilarbdialog.config.kafka.onprem;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.Properties;

import static no.nav.fo.veilarbdialog.config.kafka.onprem.KafkaOnpremConfig.CONSUMER_GROUP_ID;
import static no.nav.fo.veilarbdialog.config.kafka.onprem.KafkaOnpremConfig.PRODUCER_CLIENT_ID;

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
    Properties onPremConsumerProperties(KafkaOnpremProperties kafkaOnpremProperties, EmbeddedKafkaBroker embeddedKafka) {
        return KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(CONSUMER_GROUP_ID)
                .withBrokerUrl(embeddedKafka.getBrokersAsString())
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .withPollProperties(1, 1000)
                .build();
    }

    @Bean
    Properties onPremProducerProperties(KafkaOnpremProperties kafkaOnpremProperties, EmbeddedKafkaBroker embeddedKafka) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(PRODUCER_CLIENT_ID)
                .withBrokerUrl(embeddedKafka.getBrokersAsString())
                .withSerializers(StringSerializer.class, StringSerializer.class)
                .build();
    }

}
