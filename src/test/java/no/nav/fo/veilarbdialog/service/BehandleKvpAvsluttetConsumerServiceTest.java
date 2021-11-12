package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.fo.veilarbdialog.config.kafka.onprem.KafkaOnpremConfig;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BehandleKvpAvsluttetConsumerServiceTest {
    private static final String AKTORID = "4321";
    private static final String SAKSBEHANDLER = "Z99999";
    private static final String BEGRUNNELSE = "Derfor";
    private static final ZonedDateTime AVSLUTTETDATO = ZonedDateTime.now();

    // legge melding på kvpavsluttettopic
    // verifisere at melding er konsumert
    // verifisere behandling

    @Autowired
    KafkaProducerClient<String, String> producerClient;

    @Value("${application.kafka.kvpAvsluttetTopic}")
    String kvpAvsluttetTopic;

    @Autowired
    KafkaTestService kafkaTestService;

    @SneakyThrows
    @Test
    public void behandleKvpAvsluttetConsumerService_spiser_meldinger_fra_kvpAvsluttetTopic() {
        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = KvpAvsluttetKafkaDTO.builder()
                .aktorId(AKTORID)
                .avsluttetAv(SAKSBEHANDLER)
                .avsluttetBegrunnelse(BEGRUNNELSE)
                .avsluttetDato(AVSLUTTETDATO)
                .build();

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(kvpAvsluttetTopic, AKTORID, JsonUtils.toJson(kvpAvsluttetKafkaDTO));
        Future<RecordMetadata> recordMetadataFuture = producerClient.send(producerRecord);
        RecordMetadata recordMetadata = recordMetadataFuture.get(3, TimeUnit.SECONDS);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> kafkaTestService.erKonsumert(kvpAvsluttetTopic, KafkaOnpremConfig.CONSUMER_GROUP_ID, recordMetadata.offset()));
    }

}