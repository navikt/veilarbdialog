package no.nav.fo.veilarbdialog.service;


import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
public class KafkaDialogConfig {

    @Value("KAFKA_BROKERS_URL") // TODO: Rename.
    private String kafkaBrokersUrl;

    @Value("no.nav.modig.security.systemuser.username")
    private String username;

    @Value("no.nav.modig.security.systemuser.password")
    private String password;

    private HashMap<String, Object> kafkaProducerProperties() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + username + "\" password=\"" + password + "\";");
        props.put(ACKS_CONFIG, "all");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(CLIENT_ID_CONFIG, "veilarbdialog-producer");
        return props;
    }

    @Bean
    Producer<String, String> kafkaProducer() {
        return new KafkaProducer<>(kafkaProducerProperties());
    }
}
