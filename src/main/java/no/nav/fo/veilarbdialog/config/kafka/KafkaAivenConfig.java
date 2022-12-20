package no.nav.fo.veilarbdialog.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
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
    @Bean
    void kafkaListenerContainerFactory() {
        // org.springframework.boot.autoconfigure.kafka.KafkaAnnotationDrivenConfiguration.kafkaListenerContainerFactory
        // For aa override spring default config
    }


    // *********** produser brukernotifikasjoner START ****************
    @Bean
    <K extends SpecificRecordBase,V extends SpecificRecordBase> ProducerFactory<K, V> avroAvroProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    <K,V> KafkaTemplate<K,V> kafkaAvroAvroTemplate(ProducerFactory<K,V> avroAvroProducerFactory) {
        return new KafkaTemplate<>(avroAvroProducerFactory);
    }
    // *********** produser brukernotifikasjoner SLUTT ****************

    // ************ konsumer siste_oppfolgings_periode START ***************
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> stringStringKafkaListenerContainerFactory(
            ConsumerFactory<String, String> stringStringConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringStringConsumerFactory);
        factory.getContainerProperties()
                .setAuthExceptionRetryInterval(Duration.ofSeconds(10L));

        factory.setConcurrency(3);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(
                (rec, thr) -> log.error("Exception={} oppstått i kafka-consumer record til topic={}, partition={}, offset={}, key={} feilmelding={}",
                        thr.getClass().getSimpleName(),
                        rec.topic(),
                        rec.partition(),
                        rec.offset(),
                        rec.key(),
                        thr.getCause()
                ),
                new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)));
        return factory;
    }

    @Bean
    ConsumerFactory<String, String> stringStringConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    // ************ konsumer siste_oppfolgings_periode SLUTT ***************


    // ************ konsumer ekstern-varsel-kvittering START ***************

    @Bean
    <V extends SpecificRecordBase> ConsumerFactory<String, V> stringAvroConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    <V extends SpecificRecordBase> ConcurrentKafkaListenerContainerFactory<String, V> stringAvroKafkaListenerContainerFactory(
            ConsumerFactory<String, V> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, V> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.getContainerProperties()
                .setAuthExceptionRetryInterval(Duration.ofSeconds(10L));

        factory.setConcurrency(3);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(
                (rec, thr) -> log.error("Exception={} oppstått i kafka-consumer record til topic={}, partition={}, offset={}, bestillingsId={} feilmelding={}",
                        thr.getClass().getSimpleName(),
                        rec.topic(),
                        rec.partition(),
                        rec.offset(),
                        rec.key(),
                        thr.getCause()
                ),
                new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)));
        return factory;
    }

    // ************ konsumer ekstern-varsel-kvittering SLUTT ***************


    // ************ produser endring-paa-dialog-v1 START ***************
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

    // ************ produser endring-paa-dialog-v1 SLUTT ***************
}
