package no.nav.dialogvarsler

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

object NyDialogFlow {
    private val logger = LoggerFactory.getLogger(javaClass)
    val messageFlow = MutableSharedFlow<String>() // No-replay, hot-flow
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
    fun subscribe(jedisPool: JedisPool, channel: String) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val handler = CoroutineExceptionHandler { thread, exception ->
            logger.error("Error in kafka coroutine:", exception)
        }

        val onEvent: JedisPubSub = object:JedisPubSub() {
            override fun onMessage(channel: String?, message: String?) {
                if (message == null) return
                coroutineScope.launch {
                    messageFlow.emit(message)
                }
            }
        }
        logger.info("Setting up flow subscription...")
        coroutineScope.launch(handler) {
            logger.info("Launched coroutine for polling...")
            isStartedState.emit(true)
            jedisPool.resource.subscribe(onEvent, channel)
        }

        messageFlow
            .onEach { DialogNotifier.notifySubscribers(it) }
            .launchIn(CoroutineScope(Dispatchers.IO))
        runBlocking {
            isStartedState.first { isStarted -> isStarted }
        }
    }
}


