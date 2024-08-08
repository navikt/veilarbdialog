package no.nav.fo.veilarbdialog.kvp;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class KvpAvsluttetConsumer {

    private final DialogDataService dialogDataService;

//    @KafkaListener(topics = "${application.topic.inn.kvpavsluttet}", containerFactory = "stringStringKafkaListenerContainerFactory")
    @Timed
    void avsluttKvpPeriode(ConsumerRecord<String, String> consumerRecord) {
        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = JsonUtils.fromJson(consumerRecord.value(), KvpAvsluttetKafkaDTO.class);
        log.info("kvp periode avsluttet av " + kvpAvsluttetKafkaDTO);

        Date sluttDato = new Date(kvpAvsluttetKafkaDTO.getAvsluttetDato().toInstant().toEpochMilli());
        dialogDataService.settKontorsperredeDialogerTilHistoriske(kvpAvsluttetKafkaDTO.getAktorId(), sluttDato);
    }

}
