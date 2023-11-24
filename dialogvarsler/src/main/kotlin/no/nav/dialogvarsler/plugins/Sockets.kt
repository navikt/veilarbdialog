package no.nav.dialogvarsler.plugins

import no.nav.dialogvarsler.varsler.WsConnectionHolder.addListener
import no.nav.dialogvarsler.varsler.WsConnectionHolder.removeListener
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import no.nav.dialogvarsler.varsler.WsListener
import no.nav.dialogvarsler.varsler.WsTicketHandler
import no.nav.dialogvarsler.varsler.awaitAuthentication
import org.slf4j.LoggerFactory
import java.time.Duration

fun Application.configureSockets(ticketHandler: WsTicketHandler) {
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
            var wsListener: WsListener? = null
            try {
                wsListener = awaitAuthentication(incoming, ticketHandler)
                addListener(wsListener)
                this.send("AUTHENTICATED")
                logger.info("Authenticated")
                // Keep open until termination
                incoming.receive()
            } catch (e: ClosedReceiveChannelException) {
                logger.warn("onClose ${closeReason.await()}")
                wsListener?.let { removeListener(it) }
            } catch (e: Throwable) {
                logger.warn("onError ${closeReason.await()}", e)
                wsListener?.let { removeListener(it) }
            } finally {
                logger.info("Closing websocket connection")
            }
        }
    }
}
