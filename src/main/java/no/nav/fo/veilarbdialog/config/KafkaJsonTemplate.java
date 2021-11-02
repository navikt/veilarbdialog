package no.nav.fo.veilarbdialog.config;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class KafkaJsonTemplate<V> extends KafkaTemplate<String, V> {
    public KafkaJsonTemplate(ProducerFactory<String, V> producerFactory) {
        super(producerFactory);
    }
}
