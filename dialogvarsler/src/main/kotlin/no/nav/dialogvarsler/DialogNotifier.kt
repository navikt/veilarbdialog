package no.nav.dialogvarsler

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class EventType {
    NY_MELDING
}
@Serializable
data class DialogHendelse(
    val eventType: EventType,
    val fnr: String
)

object DialogNotifier {
    suspend fun notifySubscribers(messageString: String) {
        val message = Json.decodeFromString<DialogHendelse>(messageString)
        val websocketMessage = Json.encodeToString(message.eventType)
        WsConnectionHolder.dialogSubscriptions[message.fnr]
            ?.forEach { it.wsSession.send(websocketMessage) }
    }
}