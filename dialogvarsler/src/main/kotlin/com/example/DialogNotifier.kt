package com.example

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerRecord

@Serializable
data class SistOppdatertMessage(
    val sistOppdatert: Long
)

object DialogNotifier {
    suspend fun notifySubscribers(kafkaMessage: ConsumerRecord<String, String>) {
        val sistOppdatert = Json.decodeFromString<SistOppdatertMessage>(kafkaMessage.value())
        val fnr = kafkaMessage.key()
        val websocketMessage = Json.encodeToString(sistOppdatert)
        WsConnectionHolder.dialogSubscriptions[fnr]
            ?.forEach { it.wsSession.send(websocketMessage) }
    }
}