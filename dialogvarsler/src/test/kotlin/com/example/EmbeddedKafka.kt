package com.example

import io.github.embeddedkafka.EmbeddedKafka
import io.github.embeddedkafka.EmbeddedKafkaConfig

import org.slf4j.LoggerFactory

object EmbeddedKafkaSetup {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun start() {
        logger.info("Starting embedded Kafka...")
        EmbeddedKafka.start(EmbeddedKafkaConfig.defaultConfig())
    }

    fun stop() {
        logger.info("Stopping embedded Kafka...")
        EmbeddedKafka.stop()
    }
}