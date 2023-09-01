package com.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

object NyDialogFlow {
    private val logger = LoggerFactory.getLogger(javaClass)
    val messageFlow = MutableSharedFlow<ConsumerRecord<String, String>>() // No-replay, hot-flow
    var shuttingDown = false

    init {
        messageFlow.subscriptionCount
            .map { it != 0 }
            .distinctUntilChanged() // only react to true<->false changes
            .onEach { isActive -> // configure an action
                if (isActive)
                    logger.info("Have subscribers")
                else
                    logger.info("No subscribers")
            }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    fun stop() {
        shuttingDown = true
    }
    fun subscribe(consumer: KafkaConsumer<String, String>) {
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        logger.info("Subscribing to topic")
        coroutineScope.launch {
            logger.info("Subscribing to topic")
            while (!shuttingDown) {
                val records = consumer.poll(Duration.ofMillis(100))
                for (record in records) {
                    messageFlow.emit(record)
                    consumer.commitSync()
                }
            }
            consumer.close(Duration.ofMillis(500))
            consumer.unsubscribe()
        }

    }
}


