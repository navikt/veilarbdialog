package no.nav.fo.veilarbdialog.service;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.fo.veilarbdialog.domain.kafka.KvpAvsluttetKafkaDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BehandleKvpAvsluttetConsumerService extends TopicConsumerConfig<String, KvpAvsluttetKafkaDTO> implements TopicConsumer<String, KvpAvsluttetKafkaDTO> {

    private final DialogDataService dialogDataService;

    @Autowired
    public BehandleKvpAvsluttetConsumerService(@Lazy DialogDataService dialogDataService, @Value("${application.kafka.kvpAvsluttetTopic}") String topic) {
        this.dialogDataService = dialogDataService;
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(KvpAvsluttetKafkaDTO.class));
        this.setConsumer(this);
    }

    public void behandleKvpAvsluttet(KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO) {
        Date sluttDato = new Date(kvpAvsluttetKafkaDTO.getAvsluttetDato().toInstant().toEpochMilli());
        dialogDataService.settKontorsperredeDialogerTilHistoriske(kvpAvsluttetKafkaDTO.getAktorId(), sluttDato);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, KvpAvsluttetKafkaDTO> consumerRecord) {
        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = consumerRecord.value();
        behandleKvpAvsluttet(kvpAvsluttetKafkaDTO);
        return ConsumeStatus.OK;
    }
}
