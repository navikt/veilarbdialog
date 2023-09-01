package com.example

import io.ktor.network.sockets.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.collections.LinkedHashSet

//@Serializable
//data class ConnectionIdentifier(
//    val fnr: String,
//    val sub: String,
//    val uuid: String,
//)

typealias Fnr = String
typealias ConnectionToken = String

@Serializable
data class ConnectionTicket(
    val sub: String,
    val connectionToken: ConnectionToken,
    val fnr: Fnr
)

data class Subscription(
    val wsSession: DefaultWebSocketServerSession,
    val identifier: ConnectionTicket
)

object WsConnectionHolder {
    val wsConnectionTokenHolder = Collections.synchronizedMap<ConnectionToken, ConnectionTicket>(mutableMapOf())
    val dialogSubscriptions = Collections.synchronizedMap(mutableMapOf<Fnr, List<Subscription>>())

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
        runBlocking {
            subscription.wsSession.close(CloseReason(CloseReason.Codes.GOING_AWAY,"unsubscribing"))
        }
    }
}