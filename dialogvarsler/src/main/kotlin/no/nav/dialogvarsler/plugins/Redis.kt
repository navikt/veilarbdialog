package no.nav.dialogvarsler.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.dialogvarsler.varsler.DialogNotifier
import no.nav.dialogvarsler.varsler.IncomingDialogMessageFlow
import org.slf4j.LoggerFactory
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub
import java.lang.IllegalArgumentException

typealias PublishMessage = (NyDialogNotification) -> Long
fun Application.configureRedis(): PublishMessage {
    val logger = LoggerFactory.getLogger(Application::class.java)

    val config = this.environment.config
    val hostAndPort = config.property("redis.host").getString().split("://").last()
    val username = config.propertyOrNull("redis.username")?.getString()
    val password = config.propertyOrNull("redis.password")?.getString()
    val channel = config.property("redis.channel").getString()

    val (host, port) = hostAndPort.split(":")
        .also { if (it.size < 2) throw IllegalArgumentException("Malformed redis url") }
    val jedisPool = when {
        username != null && password != null -> JedisPool(JedisPoolConfig(), host, port.toInt(), username, password)
        else -> JedisPool(JedisPoolConfig(), host, 6379)
    }

    val subscribe = { scope: CoroutineScope, onMessage: suspend (message: String) -> Unit ->
        val eventHandler = object : JedisPubSub() {
            override fun onMessage(channel: String?, message: String?) {
                if (message == null) return
                scope.launch { onMessage(message) }
            }
        }
        jedisPool.resource.subscribe(eventHandler, channel)
    }

    IncomingDialogMessageFlow.flowOf(subscribe)
        .onEach { DialogNotifier.notifySubscribers(it) }
        .launchIn(CoroutineScope(Dispatchers.IO))

    return { message: NyDialogNotification -> jedisPool.resource.publish(channel, Json.encodeToString(message))
        .also { receivers -> logger.info("Message delivered to $receivers receivers") }
    }
}