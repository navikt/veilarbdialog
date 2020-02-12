package no.nav.fo.veilarbdialog.kafka;


import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.fo.veilarbdialog.db.dao.KafkaFeilendeMeldingerDAO;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.apache.kafka.clients.producer.KafkaProducer;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j

public class KafkaDialogProducer implements Helsesjekk {
    private final String topic;
    private final KafkaFeilendeMeldingerDAO kafkaFeilendeMeldingerDAO;
    private KafkaProducer<String, String> kafkaProducer;


    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    public static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    //private static final String KAFKA_PRODUCER_TOPIC = "aapen-fo-endringPaaDialog-v1" + "-" + getRequiredProperty(APP_ENVIRONMENT_NAME);


    public KafkaDialogProducer( KafkaFeilendeMeldingerDAO kafkaFeilendeMeldingerDAO, String topic) {
        this.kafkaFeilendeMeldingerDAO = kafkaFeilendeMeldingerDAO;
        this.topic = topic;
    }

    @Override
    public void helsesjekk() throws Throwable {

    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return null;
    }
    /*
    public void dialogEvent(String aktorId, LocalDateTime sluttdato) {
        final AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO = toDTO(aktorId, sluttdato);
        final String serialisertBruker = toJson(avsluttOppfolgingKafkaDTO);
        kafkaTemplate.send(
                topic,
                aktorId,
                serialisertBruker
        ).addCallback(
                sendResult -> onSuccess(avsluttOppfolgingKafkaDTO),
                throwable -> onError(throwable, avsluttOppfolgingKafkaDTO)
        );
    }

    public void onSuccess(AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO) {
        kafkaFeilendeMeldingerDAO.deleteAvsluttOppfolgingBruker(avsluttOppfolgingKafkaDTO.getAktorId(), avsluttOppfolgingKafkaDTO.getSluttdato());
        log.info("Bruker med aktorid {} har lagt på {}-topic", avsluttOppfolgingKafkaDTO, this.topic);
    }

    static HashMap<String, Object> kafkaProducerProperties () {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        props.put(ACKS_CONFIG, "all");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(CLIENT_ID_CONFIG, "veilarboppfolging-producer");
        return props;
    }

    static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProducerProperties());
    }

    @Transactional (propagation = Propagation.MANDATORY)
    public void onError(Throwable throwable, AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO) {
        kafkaFeilendeMeldingerDAO.insertAvsluttOppfolgingBruker(avsluttOppfolgingKafkaDTO.getAktorId(), avsluttOppfolgingKafkaDTO.getSluttdato());
        log.error("Kunne ikke publisere melding {} til {}-topic", avsluttOppfolgingKafkaDTO, this.topic, throwable);
    }


     */
}
