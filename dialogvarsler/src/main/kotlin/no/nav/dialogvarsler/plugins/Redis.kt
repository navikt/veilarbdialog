package no.nav.dialogvarsler.plugins

import io.ktor.server.application.*
import no.nav.dialogvarsler.NyDialogFlow
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig


fun Application.configureRedis() {
    val config = this.environment.config
    val host = config.property("redis.host").getString()
    val username = config.property("redis.username").getString()
    val password = config.property("redis.password").getString()
    val channel = config.property("redis.channel").getString()

    val poolConfig = JedisPoolConfig()
    val jedisPool = JedisPool(poolConfig, host, 6379, username, password)
    NyDialogFlow.subscribe(jedisPool, "")
}