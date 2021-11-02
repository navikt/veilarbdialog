package no.nav.fo.veilarbdialog.dialog_publisering;

import lombok.Builder;
import lombok.Data;
import no.nav.fo.veilarbdialog.config.KafkaJsonTemplate;
import no.nav.fo.veilarbdialog.config.kafka.aiven.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;

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
    @Ignore("Feiler fordi no.nav.fo.veilarbdialog.config.kafka.aiven.KafkaAivenTestConfig.kafkaProperties @Primary annotasjon ikke funker her")
    public void produceConsume() {
        Consumer<String, TempDto> consumer = kafkaTestService.createStringJsonConsumer(dialogTopic);

        producer.send(dialogTopic, TempDto.builder().dialogId("1").tekst("Tekst").build());

        ConsumerRecords<String, TempDto> poll = consumer.poll(Duration.ofMillis(100));

        Assertions.assertThat(poll.count()).isEqualTo(1);


    }

}
