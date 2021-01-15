package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.HashMap;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.mockito.ArgumentMatchers.any;

@TestConfiguration
@RequiredArgsConstructor
@Slf4j
public class KafkaDialogConfig {

    @Value("${application.kafka.topic}")
    private String topic;

    @Primary
    @Bean
    EmbeddedKafkaBroker embeddedKafka() {
        return new EmbeddedKafkaBroker(1);
    }

    @Bean
    Producer<String, String> kafkaProducer(EmbeddedKafkaBroker broker) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaProducer<>(props);
    }

    @Bean
    KafkaDialogService kafkaDialogService(KafkaDAO kafkaDAO,
                                          DialogDAO dialogDAO,
                                          Producer<String, String> kafkaProducer) {
        return new KafkaDialogService(kafkaDAO, dialogDAO, kafkaProducer, topic);
    }

}
