package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
public class KafkaProducerServiceTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private KafkaTestService kafkaTestService;

    @Value("${application.topic.ut.endringPaaDialog}")
    private String aivenEndringPaaDialogTopic;

    @Value("${application.kafka.endringPaaDialogTopic}")
    private String onPremEndringPaaDialogTopic;

    @Test
    public void sendDialogMelding_skalSendeTilOnPremOgAiven() {
        KafkaDialogMelding kafkaDialogMelding = KafkaDialogMelding
                .builder()
                .aktorId("123")
                .tidspunktEldsteVentende(LocalDateTime.now())
                .tidspunktEldsteUbehandlede(LocalDateTime.now())
                .build();

        var aivenConsumer  = kafkaTestService.createStringStringConsumer(aivenEndringPaaDialogTopic);
        var onPremConsumer = kafkaTestService.createStringStringConsumer(onPremEndringPaaDialogTopic);

        kafkaProducerService.sendDialogMelding(kafkaDialogMelding);

        kafkaTestService.assertHasNewRecord(aivenEndringPaaDialogTopic, aivenConsumer);
        kafkaTestService.assertHasNewRecord(onPremEndringPaaDialogTopic, onPremConsumer);
    }

}
