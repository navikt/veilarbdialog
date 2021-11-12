package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.fo.veilarbdialog.config.kafka.onprem.KafkaOnpremConfig;
import no.nav.fo.veilarbdialog.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.time.ZonedDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BehandleOppfolgingAvsluttetConsumerServiceTest {
    private static final String AKTORID = "4321";
    private static final ZonedDateTime SLUTTDATO = ZonedDateTime.now();

    @Autowired
    KafkaProducerClient<String, String> producerClient;

    @Value("${application.kafka.oppfolgingAvsluttetTopic}")
    String oppfolgingAvsluttetTopic;

    @Autowired
    KafkaTestService kafkaTestService;

    @SneakyThrows
    @Test
    public void behandleOppfolgingAvsluttetConsumerService_spiser_meldinger_fra_oppfolgingAvsluttetTopic() {
        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO = OppfolgingAvsluttetKafkaDTO.builder()
                .aktorId(AKTORID)
                .sluttdato(SLUTTDATO)
                .build();

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(oppfolgingAvsluttetTopic, AKTORID, JsonUtils.toJson(oppfolgingAvsluttetKafkaDTO));
        RecordMetadata recordMetadata = producerClient.sendSync(producerRecord);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> kafkaTestService.erKonsumert(oppfolgingAvsluttetTopic, KafkaOnpremConfig.CONSUMER_GROUP_ID, recordMetadata.offset()));
    }

}