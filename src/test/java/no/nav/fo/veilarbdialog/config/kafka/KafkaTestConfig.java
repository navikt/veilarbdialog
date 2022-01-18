package no.nav.fo.veilarbdialog.config.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

@Configuration
public class KafkaTestConfig {

    @Bean
    EmbeddedKafkaBroker embeddedKafka() {
        return new EmbeddedKafkaBroker(1, true, 1);
    }

}
