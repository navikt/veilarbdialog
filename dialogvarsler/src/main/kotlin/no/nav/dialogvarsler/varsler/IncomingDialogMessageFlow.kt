package no.nav.dialogvarsler.varsler

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory

object IncomingDialogMessageFlow {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val messageFlow = MutableSharedFlow<String>() // No-replay, hot-flow
    private val isStartedState = MutableStateFlow(false)
    private var shuttingDown = false

    init {
        messageFlow.subscriptionCount
            .map { it != 0 }
            .distinctUntilChanged() // only react to true<->false changes
            .onEach { isActive -> // configure an action
                if (isActive) logger.info("MessageFlow received subscribers")
                else logger.info("MessageFlow has no subscribers") }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    fun stop() {
        shuttingDown = true
    }
    fun flowOf(subscribe: (scope: CoroutineScope, suspend (message: String) -> Unit) -> Unit): MutableSharedFlow<String> {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val handler = CoroutineExceptionHandler { thread, exception ->
            logger.error("Error in event flow coroutine:", exception)
        }

        logger.info("Setting up flow subscription...")
        coroutineScope.launch(handler) {
            logger.info("Launched coroutine for polling...")
            isStartedState.emit(true)
            subscribe(coroutineScope) { message -> messageFlow.emit(message) }
        }

        runBlocking { isStartedState.first { isStarted -> isStarted } }
        return messageFlow
    }
}


