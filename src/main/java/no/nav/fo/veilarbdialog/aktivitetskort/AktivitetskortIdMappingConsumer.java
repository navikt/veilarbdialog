package no.nav.fo.veilarbdialog.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.domain.Arenaid;
import no.nav.fo.veilarbdialog.domain.TekniskId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AktivitetskortIdMappingConsumer {

    private final IdMappingService idMappingService;

    @Transactional
    @KafkaListener(topics = "${application.topic.inn.aktivitetskortIdMapping}", containerFactory = "stringStringKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> kafkaRecord) {
        IdMappingDTO idMapping = JsonUtils.fromJson(kafkaRecord.value(), IdMappingDTO.class);

        Arenaid arenaid = new Arenaid(idMapping.arenaId());
        TekniskId tekniskId = new TekniskId(idMapping.aktivitetId());

        idMappingService.migrerArenaDialogerTilTekniskId(arenaid, tekniskId);
    }
}
