package no.nav.dialogvarsler.plugins

import no.nav.dialogvarsler.varsler.WsConnectionHolder.addSubscription
import no.nav.dialogvarsler.varsler.WsConnectionHolder.removeSubscription
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import no.nav.dialogvarsler.varsler.ConnectionTicket
import no.nav.dialogvarsler.varsler.Subscription
import no.nav.dialogvarsler.varsler.WsConnectionHolder
import no.nav.dialogvarsler.varsler.authenticate
import java.lang.IllegalArgumentException
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") {
            var subscription: Subscription? = null
            try {
                subscription = authenticate(incoming)
                addSubscription(subscription)
                this.send("AUTHENTICATED")
                // Keep open until termination
                for (frame in incoming) {}
            } catch (e: ClosedReceiveChannelException) {
                println("onClose ${closeReason.await()}")
                subscription?.let { removeSubscription(it) }
            } catch (e: Throwable) {
                println("onError ${closeReason.await()}")
                e.printStackTrace()
                subscription?.let { removeSubscription(it) }
            }
        }
    }
}
