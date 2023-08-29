package com.example.plugins

import com.example.ConnectionIdentifier
import com.example.Subscription
import com.example.WsConnectionHolder.addSubscription
import com.example.WsConnectionHolder.removeSubscription
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
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
            this.call.request.queryParameters
            var subscription: Subscription? = null
            try {
                subscription = authenticate(incoming)
                addSubscription(subscription)
//                outgoing.send(Frame.Text("YOU SAID: $text"))
            } catch (e: ClosedReceiveChannelException) {
                println("onClose ${closeReason.await()}")
            } catch (e: Throwable) {
                println("onError ${closeReason.await()}")
                e.printStackTrace()
            } finally {
                subscription?.let { removeSubscription(it) }
            }
        }
    }
}

suspend fun DefaultWebSocketServerSession.authenticate(channel: ReceiveChannel<Frame>): Subscription {
    val connectionIdentifier = channel.receiveAsFlow()
        .map { tryAuthenticateWithMessage(it) }
        .first { it != null } ?: throw IllegalArgumentException("Failed to find auth message in websocket")
    return Subscription(
        wsSession = this,
        identifier = connectionIdentifier!!
    )
}

fun tryAuthenticateWithMessage(frame: Frame): ConnectionIdentifier? {
    try {
        if (frame !is Frame.Text) return null
        return Json.decodeFromString<ConnectionIdentifier>(frame.readText())
    } catch (e: Throwable) {
        println("Failed to deserialize ws-message")
        e.printStackTrace()
        return null
    }
}