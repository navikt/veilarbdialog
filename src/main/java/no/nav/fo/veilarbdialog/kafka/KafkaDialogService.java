package no.nav.fo.veilarbdialog.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.json.JsonUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class KafkaDialogService  {

    private static final String APP_ENVIRONMENT_NAME = "APP_ENVIRONMENT_NAME";
    private static final String KAFKA_PRODUCER_TOPIC = "aapen-fo-endringPaaDialog-v1" + "-" + getRequiredProperty(APP_ENVIRONMENT_NAME);

    private KafkaDAO kafkaDAO;
    private KafkaProducer<String, String> kafkaProducer;


    public KafkaDialogService(KafkaProducer<String, String> kafkaProducer, KafkaDAO kafkaDAO) {
        this.kafkaDAO = kafkaDAO;
        this.kafkaProducer = kafkaProducer;
    }

    public void dialogEvent (KafkaDialogMelding kafkaDialogMelding) {
        String kafkaStringMelding = JsonUtils.toJson(kafkaDialogMelding);
        ProducerRecord<String, String> kafkaMelding = new ProducerRecord<>(KAFKA_PRODUCER_TOPIC, kafkaStringMelding);
        kafkaProducer.send(kafkaMelding, (metadata, exception) -> {
            if(exception == null) {
                log.info("Bruker med aktorid {} har lagt på {}-topic", kafkaDialogMelding.getAktorId(), KAFKA_PRODUCER_TOPIC);
                int result = kafkaDAO.slettFeiletMelding(kafkaDialogMelding);
                if(result != 0 ) {
                    log.info("Sendte den feilende meldingen {} på {}-topic", kafkaDialogMelding, KAFKA_PRODUCER_TOPIC);
                }
            } else {
                log.error("Kunne ikke publisere melding  {} til {}-topic", kafkaStringMelding, KAFKA_PRODUCER_TOPIC );
                kafkaDAO.insertFeiletMelding(kafkaDialogMelding);
            }
        });
    }

    public void sendAlleFeilendeMeldinger () {
        kafkaDAO.hentAlleFeilendeMeldinger().forEach(this::dialogEvent);
    }
}
