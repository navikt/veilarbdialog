package no.nav.dialogvarsler.varsler

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException


val logger = LoggerFactory.getLogger("no.nav.dialogvarsler.varsler.WsAuth.kt")

suspend fun DefaultWebSocketServerSession.awaitAuthentication(channel: ReceiveChannel<Frame>, ticketHandler: WsTicketHandler): WsListener {
    val result = channel.receiveAsFlow()
        .map { tryAuthenticateWithMessage(it, ticketHandler) }
        .first { it is AuthResult.Success }
    return when (result) {
        is AuthResult.Success -> WsListener(
            wsSession = this,
            subscription = result.subscription
        )
        else -> throw IllegalStateException("Failed to authenticate")
    }
}


sealed class AuthResult {
    class Success(val subscription: Subscription): AuthResult()
    data object Failed: AuthResult()
}

fun tryAuthenticateWithMessage(frame: Frame, ticketHandler: WsTicketHandler): AuthResult {
    try {
        logger.info("Received ticket, trying to authenticate $frame")
        if (frame !is Frame.Text) return AuthResult.Failed
        val connectionTicket = ConnectionTicket.of(frame.readText())
        return when (connectionTicket) {
            is ValidTicket -> AuthResult.Success(ticketHandler.consumeTicket(connectionTicket))
            else -> AuthResult.Failed
        }
    } catch (e: Throwable) {
        logger.warn("Failed to deserialize ws-message", e)
        return AuthResult.Failed
    }
}
