package no.nav.dialogvarsler.varsler

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

enum class EventType {
    NY_MELDING
}
@Serializable
data class DialogHendelse(
    val eventType: EventType,
    val fnr: String
)

object DialogNotifier {
    private val logger = LoggerFactory.getLogger(javaClass)
    suspend fun notifySubscribers(messageString: String) {
        runCatching {
            val message = Json.decodeFromString<DialogHendelse>(messageString)
            val websocketMessage = Json.encodeToString(message.eventType)
            WsConnectionHolder.dialogSubscriptions[message.fnr]
                ?.forEach { it.wsSession.send(websocketMessage) }
        }.onFailure { error ->
            logger.warn("Failed to notify subscribers", error)
        }
    }
}