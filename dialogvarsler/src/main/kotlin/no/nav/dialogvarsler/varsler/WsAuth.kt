package no.nav.dialogvarsler.varsler

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import java.lang.IllegalArgumentException

suspend fun DefaultWebSocketServerSession.awaitAuthentication(channel: ReceiveChannel<Frame>): Subscription {
    val connectionTicket = channel.receiveAsFlow()
        .map { tryAuthenticateWithMessage(it) }
        .first { it != null } ?: throw IllegalArgumentException("Failed to find auth message in websocket")
    return Subscription(
        wsSession = this,
        identifier = connectionTicket
    )
}

fun tryAuthenticateWithMessage(frame: Frame): ConnectionTicket? {
    try {
        if (frame !is Frame.Text) return null
        val connectionTicket = frame.readText()
        return WsTicketHandler.consumeTicket(connectionTicket)
    } catch (e: Throwable) {
        println("Failed to deserialize ws-message")
        e.printStackTrace()
        return null
    }
}
