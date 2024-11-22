package no.nav.fo.veilarbdialog.config.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Arrays;
import java.util.Map;

@Configuration
public class KafkaAivenTestConfig {


    @Bean
    @Primary
    KafkaProperties kafkaProperties(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        kafkaProperties.setBootstrapServers(Arrays.stream(embeddedKafkaBroker.getBrokersAsString().split(",")).toList());
        return kafkaProperties;
    }

    // ******* Skrive til siste_oppfolginsperiode_topic og aktivitetskort-idmappinger i test START ***********
    @Bean
    KafkaTemplate<String, String> kafkaStringStringTemplate(ProducerFactory<String, String> stringStringProducerFactory) {
        return new KafkaTemplate<>(stringStringProducerFactory);
    }

    @Bean
    ProducerFactory<String,String> stringStringProducerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }
    // ******* Skrive til siste_oppfolginsperiode_topic og aktivitetskort-idmappinger i test SLUTT ***********

    @Bean
    public Admin kafkaAdminClient(KafkaProperties properties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> config = properties.buildAdminProperties(new DefaultSslBundleRegistry());
        config.put("bootstrap.servers", embeddedKafkaBroker.getBrokersAsString());
        return Admin.create(config);
    }
}
