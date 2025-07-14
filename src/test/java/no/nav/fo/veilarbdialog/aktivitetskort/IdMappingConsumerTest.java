package no.nav.fo.veilarbdialog.aktivitetskort;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class IdMappingConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaTemplate<String, String> stringStringKafkaTemplate;


    @Value("${application.topic.inn.aktivitetskortIdMapping}")
    String idMappingTopic;


    @Test
    @SneakyThrows
    void arena_aktivitet_skal_faa_teknisk_id_etter_migrering() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        String arenaId = "ARENATA123";
        NyMeldingDTO nyHenvendelseDTO = new NyMeldingDTO()
                .setTekst("tekst")
                .setOverskrift("En arena aktivitet")
                .setAktivitetId(arenaId);

        DialogDTO opprettetDialog = dialogTestService.opprettDialogSomBruker(mockBruker, nyHenvendelseDTO);

        Long tekniskId = 123123L;
        IdMappingDTO idMapping = new IdMappingDTO(arenaId, tekniskId, UUID.randomUUID());
        CompletableFuture<SendResult<String, String>> send = stringStringKafkaTemplate.send(idMappingTopic, JsonUtils.toJson(idMapping));
        long offset = send.get().getRecordMetadata().offset();

        kafkaTestService.assertErKonsumertAiven(idMappingTopic, offset, send.get().getRecordMetadata().partition(), 10);

        DialogDTO dialogDTO = dialogTestService.hentDialog(mockBruker, Long.parseLong(opprettetDialog.getId()));

        Assertions.assertThat(dialogDTO.getAktivitetId()).isEqualTo(tekniskId.toString());
    }



}