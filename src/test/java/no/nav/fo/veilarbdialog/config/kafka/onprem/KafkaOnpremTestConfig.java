package no.nav.fo.veilarbdialog.config.kafka.onprem;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
@Slf4j
public class KafkaOnpremTestConfig {

    @Bean
    @Primary
    public KafkaOnpremProperties kafkaOnpremProperties(EmbeddedKafkaBroker embeddedKafkaBroker) {
        KafkaOnpremProperties kafkaProperties = new KafkaOnpremProperties();

        kafkaProperties.setBrokersUrl(embeddedKafkaBroker.getBrokersAsString());
        kafkaProperties.setEndringPaaDialogTopic("aapen-fo-endringPaaDialog-v1-test");
        kafkaProperties.setKvpAvsluttetTopic("aapen-arbeidsrettetOppfolging-kvpAvsluttet-v1-test");
        kafkaProperties.setOppfolgingAvsluttetTopic("aapen-arbeidsrettetOppfolging-oppfolgingAvsluttet-v1-test");

        return kafkaProperties;
    }


    @Bean
    public KafkaProducerClient<String, String> producerClient(KafkaOnpremProperties kafkaOnpremProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(kafkaProducerProperties(kafkaOnpremProperties.getBrokersUrl()))
                .build();
    }

    private Properties kafkaProducerProperties(String brokerUrl) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

}
