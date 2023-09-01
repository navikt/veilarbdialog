package no.nav.dialogvarsler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

object NyDialogFlow {
    private val logger = LoggerFactory.getLogger(javaClass)
    val messageFlow = MutableSharedFlow<ConsumerRecord<String, String>>() // No-replay, hot-flow
    private val isStartedState = MutableStateFlow(false)
    var shuttingDown = false

    init {
        messageFlow.subscriptionCount
            .map { it != 0 }
            .distinctUntilChanged() // only react to true<->false changes
            .onEach { isActive -> // configure an action
                if (isActive)
                    logger.info("MessageFlow received subscribers")
                else
                    logger.info("Message lost all subscribers")
            }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    fun stop() {
        shuttingDown = true
    }
    fun subscribe(consumer: KafkaConsumer<String, String>) {
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        logger.info("Setting up flow subscription...")
        coroutineScope.launch {
            logger.info("Launched coroutine for polling...")
            isStartedState.emit(true)
            while (!shuttingDown) {
                val records = consumer.poll(Duration.ofMillis(100))
                if (!records.isEmpty) {
                    logger.info("Emitting ${records.count()} in messageFlow")
                }
                for (record in records) {
                    messageFlow.emit(record)
//                    consumer.commitSync()
                }
            }
            logger.info("Closing consumer...")
            consumer.close(Duration.ofMillis(500))
            consumer.unsubscribe()
        }
        runBlocking {
            isStartedState.first { isStarted -> isStarted }
        }
    }
}


