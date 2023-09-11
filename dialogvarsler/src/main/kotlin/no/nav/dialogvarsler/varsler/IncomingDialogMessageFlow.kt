package no.nav.dialogvarsler.varsler

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory

object IncomingDialogMessageFlow {
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
    fun flowOf(subscribe: (scope: CoroutineScope, suspend (message: String) -> Unit) -> Unit) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val handler = CoroutineExceptionHandler { thread, exception ->
            logger.error("Error in kafka coroutine:", exception)
        }

        logger.info("Setting up flow subscription...")
        coroutineScope.launch(handler) {
            logger.info("Launched coroutine for polling...")
            isStartedState.emit(true)
            subscribe(coroutineScope) { message -> messageFlow.emit(message) }
        }

        messageFlow
            .onEach { DialogNotifier.notifySubscribers(it) }
            .launchIn(CoroutineScope(Dispatchers.IO))
        runBlocking {
            isStartedState.first { isStarted -> isStarted }
        }
    }
}


