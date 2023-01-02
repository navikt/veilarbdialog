package no.nav.fo.veilarbdialog.kvp;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;

class KvpConsumerTest extends SpringBootTestBase {

    private static final String AKTORID = "4321";
    private static final String SAKSBEHANDLER = "Z99999";
    private static final String BEGRUNNELSE = "Derfor";
    private static final ZonedDateTime AVSLUTTETDATO = ZonedDateTime.now();

    @Autowired
    KafkaTemplate<String, String> producer;

    Consumer<String, String> springKvpAvsluttetConsumer;

    @Value("${application.topic.inn.kvpavsluttet}")
    String kvpAvsluttetTopic;

    @BeforeEach
    void setupConsumer() {
        this.springKvpAvsluttetConsumer = kafkaTestService.createStringStringConsumer(kvpAvsluttetTopic);
    }

    @SneakyThrows
    @Test
    void behandleKvpAvsluttetConsumerService_spiser_meldinger_fra_kvpAvsluttetTopic() {
        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = KvpAvsluttetKafkaDTO.builder()
                .aktorId(AKTORID)
                .avsluttetAv(SAKSBEHANDLER)
                .avsluttetBegrunnelse(BEGRUNNELSE)
                .avsluttetDato(AVSLUTTETDATO)
                .build();

        SendResult<String, String> sendResult = producer.send(kvpAvsluttetTopic, kvpAvsluttetKafkaDTO.getAktorId(), JsonUtils.toJson(kvpAvsluttetKafkaDTO)).get();

        kafkaTestService.assertErKonsumertAiven(kvpAvsluttetTopic, sendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(), 10);
    }
}
