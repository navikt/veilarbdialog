package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import org.springframework.stereotype.Service;

import java.util.Date;

@RequiredArgsConstructor
@Service
public class KafkaConsumerService {

    private final DialogDataService dialogDataService;

    public void behandleOppfolgingAvsluttet(OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto) {
        Date sluttDato = new Date(oppfolgingAvsluttetDto.getSluttdato().toInstant().toEpochMilli());
        dialogDataService.settDialogerTilHistoriske(oppfolgingAvsluttetDto.getAktorId(), sluttDato);
    }

    public void behandleKvpAvsluttet(KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO) {
        Date sluttDato = new Date(kvpAvsluttetKafkaDTO.getAvsluttetDato().toInstant().toEpochMilli());
        dialogDataService.settKontorsperredeDialogerTilHistoriske(kvpAvsluttetKafkaDTO.getAktorId(), sluttDato);
    }

}
