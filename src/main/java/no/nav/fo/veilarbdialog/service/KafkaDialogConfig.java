package no.nav.fo.veilarbdialog.service;


import no.nav.common.utils.Credentials;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
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

    @Value("${application.kafka.broker.url}")
    private String brokerUrl;

    @Value("${application.kafka.topic}")
    private String topic;

    private KafkaProducer<String, String> kafkaProducer(Credentials systemUser) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + systemUser.username + "\" password=\"" + systemUser.password + "\";");
        props.put(ACKS_CONFIG, "all");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(CLIENT_ID_CONFIG, "veilarbdialog-producer");
        return new KafkaProducer<>(props);
    }

    @Bean
    KafkaDialogService kafkaDialogService(KafkaDAO kafkaDAO,
                                          DialogDAO dialogDAO,
                                          Credentials systemUser) {
        return new KafkaDialogService(kafkaDAO, dialogDAO, kafkaProducer(systemUser), topic);
    }

}
