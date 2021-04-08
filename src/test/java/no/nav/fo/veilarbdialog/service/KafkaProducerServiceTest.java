package no.nav.fo.veilarbdialog.service;

import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import no.nav.fo.veilarbdialog.config.KafkaProperties;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import org.apache.kafka.clients.producer.MockProducer;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class KafkaProducerServiceTest {

    MockProducer<String, String> kafkaProducer = new MockProducer<>();

    KafkaDAO kafkaDAO = mock(KafkaDAO.class);

    KafkaProducerService kafkaProducerService;

    @Before
    public void setup() {
        KafkaProducerClient<String, String> producerClient = new KafkaProducerClientImpl<>(kafkaProducer);

        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setEndringPaaDialogTopic("aapen-fo-endringPaaDialog-v1-test");

        kafkaProducerService = new KafkaProducerService(kafkaProperties, producerClient, kafkaDAO, mock(DialogDAO.class));
    }

    @Test
    public void test_insert_feilende_aktorId() {
        KafkaDialogMelding melding = KafkaDialogMelding.builder()
                .aktorId("123456789")
                .tidspunktEldsteUbehandlede(LocalDateTime.now())
                .tidspunktEldsteVentende(LocalDateTime.now())
                .build();

        kafkaProducerService.sendDialogMelding(melding);
        kafkaProducer.errorNext(new RuntimeException("Failed to send record"));
        verify(kafkaDAO, times(1)).insertFeiletAktorId("123456789");
    }
}
