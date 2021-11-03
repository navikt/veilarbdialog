package no.nav.fo.veilarbdialog.dialog_publisering;

import lombok.Builder;
import lombok.Data;
import no.nav.fo.veilarbdialog.config.kafka.aiven.KafkaJsonTemplate;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class DialogerTilKafkaServiceTest {

    @Data
    @Builder
    static class TempDto {
        String dialogId;
        String tekst;
    }

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    KafkaJsonTemplate<TempDto> producer;

    @Value("topic.ut.dialog.raw")
    String dialogTopic;


    @Test
    public void verifiserKafkaAivenConfig() {
        Consumer<String, TempDto> consumer = kafkaTestService.createStringJsonConsumer(dialogTopic);

        TempDto tempDto = TempDto.builder().dialogId("1").tekst("Tekst").build();
        producer.send(dialogTopic, tempDto);

        ConsumerRecord<String, TempDto> singleRecord = getSingleRecord(consumer, dialogTopic, 500);

        Assertions.assertThat(singleRecord.value()).isEqualTo(tempDto);

    }

}
