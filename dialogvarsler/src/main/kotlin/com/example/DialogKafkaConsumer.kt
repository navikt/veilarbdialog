package com.example

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

class DialogKafkaConsumer {
    val properties = Properties().also {
        it[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092" // Replace with your Kafka broker(s)
        it[ConsumerConfig.GROUP_ID_CONFIG] = "dialogvarsler"
        it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        // Keystore config
        it[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        it[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = System.getenv("KAFKA_TRUSTSTORE_PATH")
        it[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = System.getenv("KAFKA_CREDSTORE_PASSWORD")
        // Truststore config
        it[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
        it[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = System.getenv("KAFKA_TRUSTSTORE_PATH")
        it[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = System.getenv("KAFKA_CREDSTORE_PASSWORD")
    }

    val consumer = KafkaConsumer<String, String>(properties)
    init {
        consumer.subscribe(listOf(System.getenv("ny-dialog-topic")))
    }
}

