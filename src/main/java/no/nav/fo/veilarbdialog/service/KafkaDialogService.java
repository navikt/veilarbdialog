package no.nav.fo.veilarbdialog.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import no.nav.json.JsonUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
@Component
public class KafkaDialogService  {

    private KafkaDAO kafkaDAO;
    private DialogDAO dialogDAO;
    private Producer<String, String> kafkaProducer;

    private static final String APP_ENVIRONMENT_NAME = "APP_ENVIRONMENT_NAME";
    static final String KAFKA_PRODUCER_TOPIC = "aapen-fo-endringPaaDialog-v1" + "-" + getRequiredProperty(APP_ENVIRONMENT_NAME);

    @Inject
    public KafkaDialogService(Producer<String, String> kafkaProducer, KafkaDAO kafkaDAO, DialogDAO dialogDAO) {
        this.kafkaDAO = kafkaDAO;
        this.kafkaProducer = kafkaProducer;
        this.dialogDAO = dialogDAO;
    }

    public void dialogEvent(KafkaDialogMelding kafkaDialogMelding) {
        String kafkaStringMelding = JsonUtils.toJson(kafkaDialogMelding);
        String aktorId = kafkaDialogMelding.getAktorId();
        ProducerRecord<String, String> kafkaMelding = new ProducerRecord<>(KAFKA_PRODUCER_TOPIC, aktorId, kafkaStringMelding);
        kafkaMelding.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCallIdOrRandom().getBytes()));
        kafkaProducer.send(kafkaMelding, kafkaCallbackFunction(aktorId));
    }

    private Callback kafkaCallbackFunction(String aktorId) {
        return (metadata, exception) -> {
            if(exception == null) {
                log.info("Bruker {} har lagt på {}-topic", aktorId, KAFKA_PRODUCER_TOPIC);
                kafkaDAO.slettFeiletAktorId(aktorId);
            } else {
                log.error("Kunne ikke publisere melding for bruker {} på {}-topic", aktorId, KAFKA_PRODUCER_TOPIC );
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
