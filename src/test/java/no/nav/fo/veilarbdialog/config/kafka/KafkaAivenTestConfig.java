package no.nav.fo.veilarbdialog.config.kafka;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.core.BrokerAddress;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class KafkaAivenTestConfig {


    @Bean
    @Primary
    KafkaProperties kafkaProperties(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        kafkaProperties.setBootstrapServers(Arrays.stream(embeddedKafkaBroker.getBrokerAddresses()).map(BrokerAddress::toString).collect(Collectors.toList()));
        return kafkaProperties;
    }

    // ******* Skrive til siste_oppfolginsperiode_topic i test START ***********
    @Bean
    KafkaTemplate<String, String> kafkaStringStringTemplate(ProducerFactory<String, String> stringStringProducerFactory) {
        return new KafkaTemplate<>(stringStringProducerFactory);
    }

    @Bean
    ProducerFactory<String,String> stringStringProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }
    // ******* Skrive til siste_oppfolginsperiode_topic i test SLUTT ***********


    // ******* Lese fra brukernotifikasjons topics i test START ***********
    @Bean
    <K extends SpecificRecordBase, V extends SpecificRecordBase> ConsumerFactory<K, V> avroAvroConsumerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        consumerProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }
    // ******* Lese fra brukernotifikasjons topics i test SLUTT ***********

    @Bean
    public Admin kafkaAdminClient(KafkaProperties properties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> config = properties.buildAdminProperties();
        config.put("bootstrap.servers", embeddedKafkaBroker.getBrokersAsString());
        return Admin.create(config);
    }
}
