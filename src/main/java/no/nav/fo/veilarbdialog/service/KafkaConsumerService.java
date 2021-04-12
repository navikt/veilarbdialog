package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import no.nav.fo.veilarbdialog.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class KafkaConsumerService {

    private final DialogDataService dialogDataService;

    @Autowired
    public KafkaConsumerService(@Lazy DialogDataService dialogDataService) {
        this.dialogDataService = dialogDataService;
    }

    public void behandleOppfolgingAvsluttet(OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto) {
        Date sluttDato = new Date(oppfolgingAvsluttetDto.getSluttdato().toInstant().toEpochMilli());
        dialogDataService.settDialogerTilHistoriske(oppfolgingAvsluttetDto.getAktorId(), sluttDato);
    }

    public void behandleKvpAvsluttet(KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO) {
        Date sluttDato = new Date(kvpAvsluttetKafkaDTO.getAvsluttetDato().toInstant().toEpochMilli());
        dialogDataService.settKontorsperredeDialogerTilHistoriske(kvpAvsluttetKafkaDTO.getAktorId(), sluttDato);
    }

}
