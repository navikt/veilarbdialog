package no.nav.dialogvarsler.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.dialogvarsler.varsler.*
import org.slf4j.LoggerFactory
import redis.clients.jedis.*
import java.lang.IllegalArgumentException

typealias PublishMessage = (NyDialogNotification) -> Long
typealias PingRedis = () -> String
fun Application.configureRedis(): Triple<PublishMessage, PingRedis, TicketStore> {
    val logger = LoggerFactory.getLogger(Application::class.java)

    val config = this.environment.config
    val hostAndPort = config.property("redis.host").getString().split("://").last()
    val username = config.propertyOrNull("redis.username")?.getString()
    val password = config.propertyOrNull("redis.password")?.getString()
    val channel = config.property("redis.channel").getString()

    val credentials = DefaultRedisCredentials(username, password)
    val credentialsProvider = DefaultRedisCredentialsProvider(credentials)
    val clientConfig: DefaultJedisClientConfig = DefaultJedisClientConfig.builder()
        .ssl(true)
        .credentialsProvider(credentialsProvider)
        .timeoutMillis(0)
        .build()


    val (host, port) = hostAndPort.split(":")
        .also { if (it.size < 2) throw IllegalArgumentException("Malformed redis url") }
    val redisHostAndPort = HostAndPort(host, port.toInt())
    log.info("Connecting to redis, host: $host port: $port user: $username channel: $channel")

    val jedisPool = when {
//        username != null && password != null -> JedisPool(JedisPoolConfig(), host, port.toInt(), 60000, username, password)
        username != null && password != null -> JedisPooled(redisHostAndPort, clientConfig)
        else -> {
            log.info("Fallback to local test connection (localhost) for redis")
            JedisPooled(host, 6379)
        }
    }

    val subscribe = { scope: CoroutineScope, onMessage: suspend (message: String) -> Unit ->
        val eventHandler = object : JedisPubSub() {
            override fun onMessage(channel: String?, message: String?) {
                if (message == null) return
                scope.launch { onMessage(message) }
            }
        }
        jedisPool.subscribe(eventHandler, channel)
    }

    IncomingDialogMessageFlow.flowOf(subscribe)
        .onEach { DialogNotifier.notifySubscribers(it) }
        .launchIn(CoroutineScope(Dispatchers.IO))

    val publishMessage: PublishMessage = { message: NyDialogNotification -> jedisPool.publish(channel, Json.encodeToString(message)) }
        .also { receivers -> logger.info("Message delivered to $receivers receivers") }
    val pingRedis: PingRedis = {
        jedisPool.ping()
    }

    return Triple(publishMessage, pingRedis, RedisTicketStore(jedisPool))
}

class RedisTicketStore(val jedis: JedisPooled): TicketStore {
    override fun getSubscription(ticket: ValidTicket): Subscription? {
        val value = jedis.get(ticket.value)
        return Json.decodeFromString<Subscription>(value)
    }

    override fun addSubscription(ticket: ValidTicket, subscription: Subscription) {
        jedis.set(ticket.value, Json.encodeToString(subscription))
    }

    override fun removeSubscription(ticket: ValidTicket) {
        jedis.del(ticket.value)
    }

}