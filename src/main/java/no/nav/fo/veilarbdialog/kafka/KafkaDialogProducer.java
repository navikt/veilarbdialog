package no.nav.fo.veilarbdialog.kafka;


import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.fo.veilarbdialog.db.dao.KafkaFeilendeMeldingerDAO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import static no.nav.json.JsonUtils.toJson;
@Slf4j

public class KafkaDialogProducer implements Helsesjekk {
    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaFeilendeMeldingerDAO kafkaFeilendeMeldingerDAO;


    public KafkaDialogProducer(KafkaTemplate<String, String> kafkaTemplate, KafkaFeilendeMeldingerDAO kafkaFeilendeMeldingerDAO, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaFeilendeMeldingerDAO = kafkaFeilendeMeldingerDAO;
        this.topic = topic;
    }

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

    @Transactional (propagation = Propagation.MANDATORY)
    public void onError(Throwable throwable, AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO) {
        kafkaFeilendeMeldingerDAO.insertAvsluttOppfolgingBruker(avsluttOppfolgingKafkaDTO.getAktorId(), avsluttOppfolgingKafkaDTO.getSluttdato());
        log.error("Kunne ikke publisere melding {} til {}-topic", avsluttOppfolgingKafkaDTO, this.topic, throwable);
    }

    @Override
    public void helsesjekk() throws Throwable {

    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return null;
    }
}
