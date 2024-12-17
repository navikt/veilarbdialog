package no.nav.fo.veilarbdialog.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.Map;

import static org.springframework.util.backoff.FixedBackOff.DEFAULT_INTERVAL;
import static org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS;

@Slf4j
@EnableKafka
@Configuration
public class KafkaAivenConfig {

    // ************ konsumer siste_oppfolgings_periode og aktivitetskort-idmappinger START ***************
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> stringStringKafkaListenerContainerFactory(
            @Qualifier("stringStringConsumerFactory") ConsumerFactory<String, String> stringStringConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringStringConsumerFactory);
        factory.getContainerProperties()
                .setAuthExceptionRetryInterval(Duration.ofSeconds(10L));

        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    @Bean
    DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler((rec, thr) -> log.error("Exception={} oppstått i kafka-consumer record til topic={}, partition={}, offset={}, bestillingsId={} feilmelding={}",
                thr.getClass().getSimpleName(),
                rec.topic(),
                rec.partition(),
                rec.offset(),
                rec.key(),
                thr.getCause()
        ),
                new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS));
    }

    @Bean("stringStringConsumerFactory")
    ConsumerFactory<String, String> stringStringConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties(new DefaultSslBundleRegistry());
        consumerProperties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    // ************ konsumer siste_oppfolgings_periode og aktivitetskort-idmappinger SLUTT ***************

    // ************ produser endring-paa-dialog-v1 START ***************
    @Bean
    KafkaTemplate<String, String> kafkaStringStringTemplate(ProducerFactory<String, String> stringStringProducerFactory) {
        return new KafkaTemplate<>(stringStringProducerFactory);
    }

    @Bean
    ProducerFactory<String,String> stringStringProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties(new DefaultSslBundleRegistry());
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    // ************ produser endring-paa-dialog-v1 SLUTT ***************
}
