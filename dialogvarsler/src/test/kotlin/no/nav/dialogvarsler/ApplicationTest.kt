package no.nav.dialogvarsler

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import no.nav.dialogvarsler.varsler.EventType
import no.nav.dialogvarsler.varsler.IncomingDialogMessageFlow
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory
import redis.embedded.RedisServer


class ApplicationTest : StringSpec({
    lateinit var redisServer: RedisServer
    beforeSpec {
        redisServer = RedisServer(6379)
        redisServer.start()
    }
    afterSpec {
        redisServer.stop()
        IncomingDialogMessageFlow.stop()
        server.shutdown()
    }

    "should notify subscribers" {
        testApplication {
            environment { doConfig() }
            application { module() }
            val client = createClient {
                install(WebSockets)
            }

            suspend fun getWsToken(fnr: String, sub: String): String {
                val authToken = client.post("/ws-auth-ticket") {
                    bearerAuth(server.issueToken(subject = sub).serialize())
                    contentType(ContentType.Application.Json)
                    setBody("""{ "fnr": "$fnr" }""")
                }.bodyAsText()
                authToken shouldNotBe null
                return authToken
            }

            suspend fun postMessage(fnr: String) {
                client.post("/notify-subscribers") {
                    bearerAuth(server.issueToken(subject = "Z123123").serialize())
                    contentType(ContentType.Application.Json)
                    setBody("""{ "fnr": "$fnr", "eventType": "NY_MELDING" }""")
                }.status shouldBe HttpStatusCode.OK
            }

            val veileder1 = "Z123123"
            val fnr1 = "12345678910"

            val veileder2 = "Z321321"
            val fnr2 = "11111178910"

            val veileder1token = getWsToken(fnr1, veileder1)
            val veileder2token = getWsToken(fnr2, veileder2)

            client.webSocket("/ws") {
                awaitAuth(veileder1token)
                logger.info("Posting to veilarbdialog for test-fnr 1")
                postMessage(fnr1)
                receiveStringWithTimeout().let { Json.decodeFromString<EventType>(it)  } shouldBe EventType.NY_MELDING
                logger.info("Received message, closing websocket for fnr 1")
                close(CloseReason(CloseReason.Codes.NORMAL, "Bye"))
            }
            client.webSocket("/ws") {
                awaitAuth(veileder2token)
                logger.info("Posting to veilarbdialog for test-fnr 2")
                postMessage(fnr2)
                receiveStringWithTimeout().let { Json.decodeFromString<EventType>(it)  } shouldBe EventType.NY_MELDING
                logger.info("Received message, closing websocket for fnr 2")
                close(CloseReason(CloseReason.Codes.NORMAL, "Bye"))
            }
        }
    }

//    "authorization should work" {
//        testApplication {
//            environment { doConfig() }
//            application { module() }
//            client.get("/isAlive").apply {
//                assertEquals(HttpStatusCode.OK, status)
//                assertEquals("", bodyAsText())
//            }
//            client.post("/ws-auth-ticket").apply {
//                assertEquals(HttpStatusCode.Unauthorized, status)
//            }
//            client.post("/ws-auth-ticket") {
//                bearerAuth(server.issueToken().serialize())
//                contentType(ContentType.Application.Json)
//                setBody("""{ "fnr": "12345678910" }""")
//            }.apply {
//                assertEquals(HttpStatusCode.OK, status)
//                UUID.fromString(this.bodyAsText())
//            }
//        }
//    }
    }) {

    companion object {
        private val logger = LoggerFactory.getLogger(javaClass)
        val server: MockOAuth2Server by lazy {
            MockOAuth2Server()
                .also { it.start() }
        }

        const val testTopic = "ny-dialog-topic-i-test"

        private fun ApplicationEngineEnvironmentBuilder.doConfig(
            acceptedIssuer: String = "default",
            acceptedAudience: String = "default"
        ) {
            config = MapApplicationConfig(
                "no.nav.security.jwt.issuers.size" to "1",
                "no.nav.security.jwt.issuers.0.issuer_name" to acceptedIssuer,
                "no.nav.security.jwt.issuers.0.discoveryurl" to "${server.wellKnownUrl(acceptedIssuer)}",
                "no.nav.security.jwt.issuers.0.accepted_audience" to acceptedAudience,
                "topic.ny-dialog" to testTopic,
                "redis.host" to "localhost",
                "redis.channel" to "dab.dialog-events-v1"
            )
        }
    }
}

suspend fun DefaultClientWebSocketSession.awaitAuth(token: String) {
    val logger = LoggerFactory.getLogger(javaClass)
    logger.info("Sending authtoken on websocket")
    send(Frame.Text(token))
    val authAck = (incoming.receive() as? Frame.Text)?.readText() ?: ""
    logger.info("Received auth-ack")
    authAck shouldBe "AUTHENTICATED"
}
suspend fun DefaultClientWebSocketSession.receiveStringWithTimeout(): String {
    return withTimeout(500) {
        (incoming.receive() as? Frame.Text)?.readText() ?: ""
    }
}