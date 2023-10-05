package no.nav.fo.veilarbdialog.manuellStatus;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ManuellStatusConsumerTest extends SpringBootTestBase {
    @Autowired
    KafkaTemplate<String, String> stringStringKafkaTemplate;

    @Value("${application.topic.inn.manuellStatusEndring}")
    String manuellStatusEndringTopic;

    @Test
    @SneakyThrows
    void arena_aktivitet_skal_faa_teknisk_id_etter_migrering() {
        // Gitt bruker med en dialog som venter på svar fra NAV
        var bruker = MockNavService.createHappyBruker();
        var veileder = MockNavService.createVeileder(bruker);
        var nyHenvendelseDTO = new NyHenvendelseDTO()
            .setTekst("tekst")
            .setVenterPaaSvarFraNav(false);
        var opprettetDialog = dialogTestService.opprettDialogSomVeileder(veileder, bruker, nyHenvendelseDTO);
        // GUI-støtter ikke å sette Venter på NAV ved opprettelse så gjøres i 2-step i testene også
        dialogTestService.setFerdigBehandlet(veileder, Long.parseLong(opprettetDialog.getId()), false);
        var dialogBefore = dialogTestService.hentDialog(bruker, Long.parseLong(opprettetDialog.getId()));
        Assertions.assertThat(dialogBefore.isVenterPaSvar()).isFalse();

        // Og bruker settes til manuell/blir reservert i KRR
        var manuellStatusEndring = new ManuellStatusEndring(bruker.getAktorId(), true);
        CompletableFuture<SendResult<String, String>> send = stringStringKafkaTemplate.send(manuellStatusEndringTopic, JsonUtils.toJson(manuellStatusEndring));
        long offset = send.get().getRecordMetadata().offset();
        kafkaTestService.assertErKonsumertAiven(manuellStatusEndringTopic, offset, send.get().getRecordMetadata().partition(), 10);

        // SKal dialog være ferdig behandlet
        var dialogAfter = dialogTestService.hentDialog(bruker, Long.parseLong(opprettetDialog.getId()));
        Assertions.assertThat(dialogAfter.isFerdigBehandlet()).isTrue();
    }
}
