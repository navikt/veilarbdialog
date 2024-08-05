package no.nav.fo.veilarbdialog.config.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

@Configuration
public class KafkaTestConfig {

    @Bean
    EmbeddedKafkaBroker embeddedKafka() {
        return new EmbeddedKafkaKraftBroker(1, 1);
    }

}
