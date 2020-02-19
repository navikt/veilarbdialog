package no.nav.fo.veilarbdialog.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.json.JsonUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
@Component
public class KafkaDialogService  {

    private KafkaDAO kafkaDAO;
    private DialogDAO dialogDAO;
    private Producer<String, String> kafkaProducer;

    private static final String APP_ENVIRONMENT_NAME = "APP_ENVIRONMENT_NAME";
    static final String KAFKA_PRODUCER_TOPIC = "aapen-pto-endringPaaDialog-v1" + "-" + getRequiredProperty(APP_ENVIRONMENT_NAME);

    @Inject
    public KafkaDialogService(Producer<String, String> kafkaProducer, KafkaDAO kafkaDAO, DialogDAO dialogDAO) {
        this.kafkaDAO = kafkaDAO;
        this.kafkaProducer = kafkaProducer;
        this.dialogDAO = dialogDAO;
    }

    public void dialogEvent (KafkaDialogMelding kafkaDialogMelding) {
        String kafkaStringMelding = JsonUtils.toJson(kafkaDialogMelding);
        ProducerRecord<String, String> kafkaMelding = new ProducerRecord<>(KAFKA_PRODUCER_TOPIC, kafkaStringMelding);
        kafkaProducer.send(kafkaMelding, (metadata, exception) -> {
            if(exception == null) {
                log.info("Bruker med aktorid {} har lagt på {}-topic", kafkaDialogMelding.getAktorId(), KAFKA_PRODUCER_TOPIC);
                int result = kafkaDAO.slettFeiletAktorId(kafkaDialogMelding.getAktorId());
                if(result != 0 ) {
                    log.info("Sendte den feilende meldingen {} på {}-topic", kafkaDialogMelding, KAFKA_PRODUCER_TOPIC);
                }
            } else {
                log.error("Kunne ikke publisere melding  {} til {}-topic", kafkaStringMelding, KAFKA_PRODUCER_TOPIC );
                kafkaDAO.insertFeiletAktorId(kafkaDialogMelding.getAktorId());
            }
        });
    }

    public void sendAlleFeilendeMeldinger () {
        kafkaDAO.hentAlleFeilendeAktorId()
                .stream()
                .map(aktorId -> {
                    List<DialogData> dialoger =  dialogDAO.hentDialogerForAktorId(aktorId);
                    return KafkaDialogMelding.mapTilDialogData(dialoger, aktorId);
                })
                .collect(Collectors.toList()).forEach(this::dialogEvent);
    }
}
