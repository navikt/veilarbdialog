package no.nav.dialogvarsler.plugins

import no.nav.dialogvarsler.varsler.WsConnectionHolder.addSubscription
import no.nav.dialogvarsler.varsler.WsConnectionHolder.removeSubscription
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import no.nav.dialogvarsler.varsler.Subscription
import no.nav.dialogvarsler.varsler.awaitAuthentication
import org.slf4j.LoggerFactory
import java.time.Duration

fun Application.configureSockets() {
    val logger = LoggerFactory.getLogger(javaClass)

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") {
            logger.info("Opening websocket connection")
            var subscription: Subscription? = null
            try {
                subscription = awaitAuthentication(incoming)
                addSubscription(subscription)
                this.send("AUTHENTICATED")
                // Keep open until termination
                incoming.receive()
            } catch (e: ClosedReceiveChannelException) {
                logger.warn("onClose ${closeReason.await()}")
                subscription?.let { removeSubscription(it) }
            } catch (e: Throwable) {
                logger.warn("onError ${closeReason.await()}")
                e.printStackTrace()
                subscription?.let { removeSubscription(it) }
            } finally {
                logger.info("Closing websocket connection")
            }
        }
    }
}
