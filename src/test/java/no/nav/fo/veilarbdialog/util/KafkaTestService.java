package no.nav.fo.veilarbdialog.util;

import lombok.SneakyThrows;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Service
public class KafkaTestService {

    public static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(5);
    private final ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory;
    private final ConsumerFactory<String, String> stringStringConsumerFactory;
    private final Admin kafkaAdminClient;
    private @Value("${spring.kafka.consumer.group-id}") String aivenGroupId;

    public KafkaTestService(
            @Qualifier("avroAvroConsumerFactory") ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory,
            @Qualifier("stringStringConsumerFactory") ConsumerFactory<String, String> stringStringConsumerFactory,
            Admin kafkaAdminClient
    ) {
        this.avroAvroConsumerFactory = avroAvroConsumerFactory;
        this.stringStringConsumerFactory = stringStringConsumerFactory;
        this.kafkaAdminClient = kafkaAdminClient;
    }


    public Consumer createAvroAvroConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        Consumer newConsumer = avroAvroConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
        seekToEnd(topic, newConsumer);
        return newConsumer;
    }

    public Consumer createStringStringConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        Consumer newConsumer = stringStringConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
        seekToEnd(topic, newConsumer);
        return newConsumer;
    }



    private void seekToEnd(String topic, Consumer newConsumer) {
        var topicPartitions = List.of(new TopicPartition(topic, 0));
        newConsumer.assign(topicPartitions);
        newConsumer.seekToEnd(topicPartitions);
        topicPartitions.forEach(topicPartition -> newConsumer.position(topicPartition, Duration.ofSeconds(10)));
        newConsumer.commitSync(Duration.ofSeconds(10));
    }

    public void assertHasNewRecord(String topic, Consumer consumer) {
        await().atMost(10, SECONDS).until(() -> {
            var records = consumer.poll(Duration.ofMillis(10));
            return records.records(topic).iterator().hasNext();
        });
    }


    public void assertErKonsumertAiven(String topic, long producerOffset, int partition, int timeOutSeconds) {
        await().atMost(timeOutSeconds, SECONDS).until(() -> erKonsumert(topic, aivenGroupId, producerOffset, partition));
    }

    public void assertErKonsumertAiven(String topic, long producerOffset, int partition) {
        await().atMost(DEFAULT_WAIT_TIMEOUT).until(() -> erKonsumert(topic, aivenGroupId, producerOffset, partition));
    }

    @SneakyThrows
    public boolean erKonsumert(String topic, String groupId, long producerOffset, int partition) {
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = kafkaAdminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();
        OffsetAndMetadata offsetAndMetadata = topicPartitionOffsetAndMetadataMap.get(new TopicPartition(topic, partition));

        if (offsetAndMetadata == null) {
            return false;
        }

        long commitedOffset = offsetAndMetadata.offset();
        return commitedOffset >= producerOffset;
    }

    @SneakyThrows
    public boolean harKonsumertAlleMeldinger(String topic, Consumer consumer) {
        consumer.commitSync();
        String groupId = consumer.groupMetadata().groupId();
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = kafkaAdminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();
        OffsetAndMetadata offsetAndMetadata = topicPartitionOffsetAndMetadataMap.get(new TopicPartition(topic, 0));

        if (offsetAndMetadata == null) {
            // Hvis ingen commitede meldinger, så er alt konsumert
            return true;
        }

        List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
        List<TopicPartition> collect = partitionInfos.stream().map(f -> new TopicPartition(topic, f.partition())).collect(Collectors.toList());

        Map<TopicPartition, Long> map = consumer.endOffsets(collect);
        Long endOffset = map.get(collect.get(0));

        return offsetAndMetadata.offset() == endOffset;
    }
}
