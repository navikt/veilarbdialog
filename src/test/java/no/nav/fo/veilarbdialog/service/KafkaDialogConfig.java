package no.nav.fo.veilarbdialog.service;

import org.apache.kafka.clients.producer.Producer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestConfiguration
public class KafkaDialogConfig {

    @MockBean
    Producer<String, String> kafkaProducer;

    @MockBean
    KafkaDialogService kafkaDialogService;

    @Bean
    Producer<String, String> kafkaProducer() {
        return kafkaProducer;
    }

    @Bean
    KafkaDialogService kafkaDialogService() {
        return kafkaDialogService;
    }

}
