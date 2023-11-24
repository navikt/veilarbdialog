package no.nav.dialogvarsler.varsler

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.util.*

typealias Fnr = String
//typealias ConnectionTicket = String

@Serializable
data class Subscription(
    val sub: String,
    val connectionTicket: String,
    val fnr: Fnr
)

data class WsListener(
    val wsSession: DefaultWebSocketServerSession,
    val subscription: Subscription
)

object WsConnectionHolder {
    val dialogListeners = Collections.synchronizedMap(mutableMapOf<Fnr, List<WsListener>>())

    fun addListener(wsListener: WsListener) {
        val currentSubscriptions = dialogListeners[wsListener.subscription.fnr]
        val newWsListeners: List<WsListener> = currentSubscriptions
            ?.let { it + listOf(wsListener) } ?: listOf(wsListener)
        dialogListeners[wsListener.subscription.fnr] = newWsListeners
    }
    fun removeListener(wsListener: WsListener) {
        val currentSubscriptions = dialogListeners[wsListener.subscription.fnr]
        val newWsListeners: List<WsListener> = currentSubscriptions
            ?.filter { it.subscription == wsListener.subscription } ?: emptyList()
        dialogListeners[wsListener.subscription.fnr] = newWsListeners
        runBlocking {
            wsListener.wsSession.close(CloseReason(CloseReason.Codes.GOING_AWAY,"unsubscribing"))
        }
    }
}