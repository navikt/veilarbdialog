package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

class KafkaProducerServiceTest extends SpringBootTestBase {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private KafkaTestService kafkaTestService;

    @Value("${application.topic.ut.endringPaaDialog}")
    private String aivenEndringPaaDialogTopic;

    @Test
    void sendDialogMelding_skalSendeTilTopic() {
        KafkaDialogMelding kafkaDialogMelding = KafkaDialogMelding
                .builder()
                .aktorId("123")
                .tidspunktEldsteVentende(LocalDateTime.now())
                .tidspunktEldsteUbehandlede(LocalDateTime.now())
                .build();

        var kafkaConsumer = kafkaTestService.createStringStringConsumer(aivenEndringPaaDialogTopic);

        kafkaProducerService.sendDialogMelding(kafkaDialogMelding);

        kafkaTestService.assertHasNewRecord(aivenEndringPaaDialogTopic, kafkaConsumer);
    }

}
