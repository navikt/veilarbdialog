package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

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

    @SneakyThrows
    @Test
    public void kanari() {

        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = KvpAvsluttetKafkaDTO.builder()
                .aktorId(AKTORID)
                .avsluttetAv(SAKSBEHANDLER)
                .avsluttetBegrunnelse(BEGRUNNELSE)
                .avsluttetDato(AVSLUTTETDATO)
                .build();

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(kvpAvsluttetTopic, AKTORID, JsonUtils.toJson(kvpAvsluttetKafkaDTO));
        Future<RecordMetadata> recordMetadataFuture = producerClient.send(producerRecord);
        RecordMetadata recordMetadata = recordMetadataFuture.get(3, TimeUnit.SECONDS);

    }

}