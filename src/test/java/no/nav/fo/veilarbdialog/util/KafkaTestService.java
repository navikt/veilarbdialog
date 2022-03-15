package no.nav.fo.veilarbdialog.util;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
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
@RequiredArgsConstructor
public class KafkaTestService {

    private final ConsumerFactory<Object, Object> avroAvroConsumerFactory;
    private final Admin kafkaAdminClient;
    private @Value("${spring.kafka.consumer.group-id}") String aivenGroupId;

    public Consumer createAvroAvroConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        Consumer newConsumer = avroAvroConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
        seekToEnd(topic, newConsumer);
        return newConsumer;
    }

    public void seekToEnd(String topic, Consumer newConsumer) {
        List<PartitionInfo> partitionInfos = newConsumer.partitionsFor(topic);
        List<TopicPartition> collect = partitionInfos.stream().map(f -> new TopicPartition(topic, f.partition())).collect(Collectors.toList());

        newConsumer.assign(collect);
        newConsumer.seekToEnd(collect);

        collect.forEach(a -> newConsumer.position(a, Duration.ofSeconds(10)));

        newConsumer.commitSync(Duration.ofSeconds(10));
    }

    public void assertErKonsumertAiven(String topic, long producerOffset, int partition, int timeOutSeconds) {
        await().atMost(timeOutSeconds, SECONDS).until(() -> erKonsumert(topic, aivenGroupId, producerOffset,partition));
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
