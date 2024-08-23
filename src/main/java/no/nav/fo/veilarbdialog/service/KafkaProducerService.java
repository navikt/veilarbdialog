package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.filter.LogRequestFilter;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> stringStringKafkaTemplate;

    @Value("${application.topic.ut.endringPaaDialog}")
    private String endringPaaDialogTopic;

    public void sendDialogMelding(KafkaDialogMelding kafkaDialogMelding) {
        var kafkaStringMelding = JsonUtils.toJson(kafkaDialogMelding);
        String aktorId = kafkaDialogMelding.getAktorId();

        sendSync(opprettKafkaMelding(endringPaaDialogTopic, aktorId, kafkaStringMelding));
    }

    private static String getCallIdOrRandom() {
        return Optional.ofNullable(MDC.get(LogRequestFilter.NAV_CALL_ID_HEADER_NAME))
                .orElse(IdUtils.generateId());
    }


    @SneakyThrows
    private void sendSync(ProducerRecord<String, String> producerRecord) {
        stringStringKafkaTemplate.send(producerRecord).get();
    }

    private ProducerRecord<String, String> opprettKafkaMelding(String topic, String key, String value) {
        ProducerRecord<String, String> kafkaMelding = new ProducerRecord<>(topic, key, value);
        kafkaMelding.headers().add(new RecordHeader(LogRequestFilter.NAV_CALL_ID_HEADER_NAME, getCallIdOrRandom().getBytes()));
        return kafkaMelding;
    }
}
