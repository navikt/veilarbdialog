package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@RequiredArgsConstructor
@Slf4j
public class KafkaDialogService {

    private final KafkaDAO kafkaDAO;
    private final DialogDAO dialogDAO;
    private final Producer<String, String> kafkaProducer;
    private final String topic;

    public void dialogEvent(KafkaDialogMelding kafkaDialogMelding) {
        String kafkaStringMelding = JsonUtils.toJson(kafkaDialogMelding);
        String aktorId = kafkaDialogMelding.getAktorId();
        ProducerRecord<String, String> kafkaMelding = new ProducerRecord<>(topic, aktorId, kafkaStringMelding);
        kafkaMelding.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCallIdOrRandom().getBytes()));
        kafkaProducer.send(kafkaMelding, kafkaCallbackFunction(aktorId));
    }

    private Callback kafkaCallbackFunction(String aktorId) {
        return (metadata, exception) -> {
            if (exception == null) {
                log.info("Bruker {} har lagt på {}-topic", aktorId, topic);
                kafkaDAO.slettFeiletAktorId(aktorId);
            } else {
                log.error("Kunne ikke publisere melding for bruker {} på {}-topic", aktorId, topic);
                kafkaDAO.insertFeiletAktorId(aktorId);
            }
        };
    }

    public void sendAlleFeilendeMeldinger() {
        kafkaDAO.hentAlleFeilendeAktorId()
                .stream()
                .map(aktorId -> {
                    List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
                    return KafkaDialogMelding.mapTilDialogData(dialoger, aktorId);
                })
                .collect(Collectors.toList())
                .forEach(this::dialogEvent);
    }

    private String getCallIdOrRandom() {
        return Optional.ofNullable(MDC.get(PREFERRED_NAV_CALL_ID_HEADER_NAME))
                .orElse(IdUtils.generateId());
    }
}
