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

suspend fun DefaultWebSocketServerSession.awaitAuthentication(channel: ReceiveChannel<Frame>): Subscription {
    val result = channel.receiveAsFlow()
        .map { tryAuthenticateWithMessage(it) }
        .first { it is AuthResult.Success }
    return when (result) {
        is AuthResult.Success -> Subscription(
            wsSession = this,
            identifier = result.connectionTicket
        )
        else -> throw IllegalStateException("Failed to authenticate")
    }
}


sealed class AuthResult {
    class Success(val connectionTicket: ConnectionTicket): AuthResult()
    data object Failed: AuthResult()
}

fun tryAuthenticateWithMessage(frame: Frame): AuthResult {
    try {
        logger.info("Received ticket, trying to authenticate")
        if (frame !is Frame.Text) return AuthResult.Failed
        val connectionTicket = frame.readText()
        return AuthResult.Success(WsTicketHandler.consumeTicket(connectionTicket))
    } catch (e: Throwable) {
        logger.warn("Failed to deserialize ws-message", e)
        return AuthResult.Failed
    }
}
