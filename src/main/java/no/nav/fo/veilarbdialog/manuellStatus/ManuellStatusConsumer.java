package no.nav.fo.veilarbdialog.manuellStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.domain.Person;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.fo.veilarbdialog.service.DialogStatusService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManuellStatusConsumer {

    private final DialogStatusService dialogStatusService;
    private final DialogDataService dialogDataService;

    @Transactional
    @KafkaListener(topics = "${application.topic.inn.manuellStatusEndring}", containerFactory = "stringStringKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> kafkaRecord) {
        ManuellStatusEndring manuellStatusEndring = JsonUtils.fromJson(kafkaRecord.value(), ManuellStatusEndring.class);
        var aktorId = Person.aktorId(manuellStatusEndring.aktorId());
        var dialoger = dialogDataService.hentDialogerForBruker(aktorId);
        dialoger.forEach((dialog) -> {
            if (dialog.erFerdigbehandlet()) return;
            // Sett alle dialoger som "Venter på svar fra NAV" til ferdigbehandlet
            dialogStatusService.oppdaterVenterPaNavSiden(dialog, true);
        });
        dialogDataService.sendPaaKafka(aktorId.get());
    }
}
