package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarbdialog.config.KafkaOnpremProperties;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducerService {

    private final KafkaOnpremProperties kafkaProperties;

    private final KafkaProducerClient<String, String> producerClient;

    private final KafkaDAO kafkaDAO;

    private final DialogDAO dialogDAO;

    public void sendDialogMelding(KafkaDialogMelding kafkaDialogMelding) {
        var kafkaStringMelding = JsonUtils.toJson(kafkaDialogMelding);
        String aktorId = kafkaDialogMelding.getAktorId();
        String topic = kafkaProperties.getEndringPaaDialogTopic();

        ProducerRecord<String, String> kafkaMelding = new ProducerRecord<>(topic, aktorId, kafkaStringMelding);
        kafkaMelding.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCallIdOrRandom().getBytes()));

        producerClient.sendSync(kafkaMelding);
    }

    public void sendAlleFeilendeMeldinger() {
        kafkaDAO.hentAlleFeilendeAktorId()
                .stream()
                .map(aktorId -> {
                    List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
                    return KafkaDialogMelding.mapTilDialogData(dialoger, aktorId);
                })
                .collect(Collectors.toList())
                .forEach(this::sendDialogMelding);
    }

    private static String getCallIdOrRandom() {
        return Optional.ofNullable(MDC.get(PREFERRED_NAV_CALL_ID_HEADER_NAME))
                .orElse(IdUtils.generateId());
    }

}
