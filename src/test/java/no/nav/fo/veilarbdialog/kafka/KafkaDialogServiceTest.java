package no.nav.fo.veilarbdialog.kafka;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import org.apache.kafka.clients.producer.MockProducer;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class KafkaDialogServiceTest {

    MockProducer kafkaProducer = new MockProducer();
    KafkaDAO kafkaDAO = mock(KafkaDAO.class);
    KafkaDialogService kafkaDialogService;

    @Before
    public void setup(){
        System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");
        kafkaDialogService = new KafkaDialogService(kafkaProducer, kafkaDAO,   mock(DialogDAO.class));
    }

    @Test
    public void test_insert_feilende_aktorId() {
        KafkaDialogMelding melding =  KafkaDialogMelding.builder()
                .aktorId("123456789")
                .tidspunktEldsteUbehandlede(LocalDateTime.now())
                .tidspunktEldsteVentende(LocalDateTime.now())
                .build();

        kafkaDialogService.dialogEvent(melding);
        kafkaProducer.errorNext(new RuntimeException("Failed to send record"));
        verify(kafkaDAO, times(1)).insertFeiletAktorId("123456789");
    }
}
