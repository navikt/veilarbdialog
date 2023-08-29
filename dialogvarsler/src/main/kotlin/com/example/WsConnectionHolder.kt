package com.example

import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionIdentifier(
    val fnr: String,
    val sub: String,
    val uuid: String,
)

typealias Fnr = String
typealias ConnectionToken = String

data class ConnectionTicket(
    val sub: String,
    val connectionToken: ConnectionToken,
    val fnr: Fnr
)

data class Subscription(
    val wsSession: DefaultWebSocketServerSession,
    val identifier: ConnectionIdentifier
)

object WsConnectionHolder {
    val wsConnectionTokenHolder = mutableMapOf<ConnectionToken, ConnectionTicket>()
    val dialogSubscriptions = mutableMapOf<Fnr, List<Subscription>>()

    fun addSubscription(subscription: Subscription) {
        val currentSubscriptions = dialogSubscriptions[subscription.identifier.fnr]
        val newSubscriptions: List<Subscription> = currentSubscriptions
            ?.let { it + listOf(subscription) } ?: listOf(subscription)
        dialogSubscriptions[subscription.identifier.fnr] = newSubscriptions
    }
    fun removeSubscription(subscription: Subscription) {
        val currentSubscriptions = dialogSubscriptions[subscription.identifier.fnr]
        val newSubscriptions: List<Subscription> = currentSubscriptions
            ?.filter { it.identifier == subscription.identifier } ?: emptyList()
        dialogSubscriptions[subscription.identifier.fnr] = newSubscriptions
    }
}