package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class EksternVarslingKvitteringConsumer {
    private final KvitteringDAO kvitteringDAO;

    private final BrukernotifikasjonRepository brukernotifikasjonRepository;
    private final KvitteringMetrikk kvitteringMetrikk;

    public static final String FEILET = "FEILET";
    public static final String INFO = "INFO";
    public static final String OVERSENDT = "OVERSENDT";
    public static final String FERDIGSTILT = "FERDIGSTILT";
    private final String oppgavePrefix;
    private final String beskjedPrefix;
    private final String appname;

    public EksternVarslingKvitteringConsumer(KvitteringDAO kvitteringDAO , BrukernotifikasjonRepository brukernotifikasjonRepository, KvitteringMetrikk kvitteringMetrikk, @Value("${spring.application.name}") String appname) {
        this.kvitteringDAO = kvitteringDAO;
        this.brukernotifikasjonRepository = brukernotifikasjonRepository;
        this.kvitteringMetrikk = kvitteringMetrikk;
        oppgavePrefix = "O-" + appname + "-";
        beskjedPrefix = "B-" + appname + "-";
        this.appname = appname;
    }


    @Transactional
    @KafkaListener(topics = "${application.topic.inn.eksternVarselKvittering}", containerFactory = "stringAvroKafkaListenerContainerFactory", autoStartup = "${app.kafka.enabled:true}")
    public void consume(ConsumerRecord<String, DoknotifikasjonStatus> kafkaRecord) {
        DoknotifikasjonStatus melding = kafkaRecord.value();
        if (!appname.equals(melding.getBestillerId())) {
            return;
        }

        String brukernotifikasjonBestillingsId = melding.getBestillingsId();
        log.info("Konsumerer DoknotifikasjonStatus bestillingsId={}, status={}", brukernotifikasjonBestillingsId, melding.getStatus());

        String bestillingsId = utledBestillingsId(brukernotifikasjonBestillingsId);

        if (!brukernotifikasjonRepository.finnesBrukernotifikasjon(bestillingsId)) {
            log.warn("Mottok kvittering for brukernotifikasjon bestillingsid={} som ikke finnes i våre systemer", bestillingsId);
            throw new IllegalArgumentException("Ugyldig bestillingsid.");
        }

        kvitteringDAO.lagreKvittering(bestillingsId, melding);

        String status = melding.getStatus();

        switch (status) {
            case INFO, OVERSENDT:
                break;
            case FEILET:
                log.error("varsel feilet for notifikasjon bestillingsId={} med melding {}", brukernotifikasjonBestillingsId, melding.getMelding());
                brukernotifikasjonRepository.setEksternVarselFeilet(bestillingsId);
                break;
            case FERDIGSTILT:
                if (melding.getDistribusjonId() != null) {
                    // Kan komme første gang og på resendinger
                    brukernotifikasjonRepository.setEksternVarselSendtOk(bestillingsId);
                    log.info("Brukernotifikasjon fullført for bestillingsId={}", brukernotifikasjonBestillingsId);
                } else {
                    log.info("Hele bestillingen inkludert revarsling er ferdig, bestillingsId={}", brukernotifikasjonBestillingsId);
                }
                break;
            default:
                log.error("ukjent status for melding {}", melding);
                throw new IllegalArgumentException("ukjent status for melding");
        }

        List<Kvittering> kvitterings = kvitteringDAO.hentKvitteringer(bestillingsId);
        log.info("EksternVarsel Kvitteringshistorikk {}", kvitterings);

        kvitteringMetrikk.incrementBrukernotifikasjonKvitteringMottatt(status);
    }
    private String utledBestillingsId(String inputBestillingsid) {
        if (!inputBestillingsid.startsWith(oppgavePrefix) && !inputBestillingsid.startsWith(beskjedPrefix)) {
            return inputBestillingsid;
        } else {
            return inputBestillingsid.substring(oppgavePrefix.length()); // Fjerner O eller B + - + srv + - som legges til av brukernotifikajson
        }
    }
}
