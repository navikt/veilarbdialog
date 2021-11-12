package no.nav.fo.veilarbdialog.service;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.fo.veilarbdialog.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BehandleOppfolgingAvsluttetConsumerService extends TopicConsumerConfig<String, OppfolgingAvsluttetKafkaDTO> implements TopicConsumer<String, OppfolgingAvsluttetKafkaDTO> {

    private final DialogDataService dialogDataService;

    public BehandleOppfolgingAvsluttetConsumerService(@Value("${application.kafka.oppfolgingAvsluttetTopic}") String topic, @Lazy DialogDataService dialogDataService) {
        this.dialogDataService = dialogDataService;
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(OppfolgingAvsluttetKafkaDTO.class));
        this.setConsumer(this);
    }

    public void behandleOppfolgingAvsluttet(OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto) {
        Date sluttDato = new Date(oppfolgingAvsluttetDto.getSluttdato().toInstant().toEpochMilli());
        dialogDataService.settDialogerTilHistoriske(oppfolgingAvsluttetDto.getAktorId(), sluttDato);
    }


    @Override
    public ConsumeStatus consume(ConsumerRecord<String, OppfolgingAvsluttetKafkaDTO> consumerRecord) {
        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO = consumerRecord.value();
        behandleOppfolgingAvsluttet(oppfolgingAvsluttetKafkaDTO);
        return ConsumeStatus.OK;
    }
}
