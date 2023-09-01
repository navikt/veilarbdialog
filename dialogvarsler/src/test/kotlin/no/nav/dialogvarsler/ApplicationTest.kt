package no.nav.dialogvarsler

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.test.assertEquals
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.withTimeout

class ApplicationTest : StringSpec({
    afterSpec {
        NyDialogFlow.stop()
        EmbeddedKafkaSetup.stop()
        server.shutdown()
    }

    "kafka should work" {
        testApplication {
            environment { doConfig() }
            application { module() }
            val client = createClient {
                install(WebSockets)
            }

            suspend fun getToken(fnr: String, sub: String): String {
                val authToken = client.post("/ws-auth-ticket") {
                    bearerAuth(server.issueToken(subject = sub).serialize())
                    contentType(ContentType.Application.Json)
                    setBody("""{ "fnr": "$fnr" }""")
                }.bodyAsText()
                authToken shouldNotBe null
                return authToken
            }

            val veileder1 = "Z123123"
            val fnr1 = "12345678910"

            val veileder2 = "Z321321"
            val fnr2 = "11111178910"

            val producer = getTestProducer()
            val messageToSend = """{ "sistOppdatert": 1693510558103 }"""
            val fnr1Record = ProducerRecord(testTopic, fnr1, messageToSend)
            val fnr2Record = ProducerRecord(testTopic, fnr2, messageToSend)
            val veileder1token = getToken(fnr1, veileder1)
            val veileder2token = getToken(fnr2, veileder2)

            client.webSocket("/ws") {
                awaitAuth(veileder1token)
                logger.info("Pushing kafka message for test-fnr 1")
                producer.send(fnr1Record).get()
                receiveStringWithTimeout() shouldBe """{"sistOppdatert":1693510558103}"""
                logger.info("Received message, closing websocket for fnr 1")
                close(CloseReason(CloseReason.Codes.NORMAL, "Bye"))
            }
            client.webSocket("/ws") {
                awaitAuth(veileder2token)
                logger.info("Pushing kafka message for test-fnr 2")
                producer.send(fnr2Record).get()
                receiveStringWithTimeout() shouldBe """{"sistOppdatert":1693510558103}"""
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
        var kafkaBrokerServer = EmbeddedKafkaSetup.start()
        val server: MockOAuth2Server by lazy {
            MockOAuth2Server()
                .also { it.start() }
        }
        private fun getBrokerConnectionString(): String {
            return kafkaBrokerServer?.broker()?.createBrokerInfo()
                ?.broker()?.endPoints()?.find { true }?.get()?.connectionString()
                ?: throw Exception("Could not find brokerserver")
        }
        private fun getTestProducer(): KafkaProducer<String, String> {
            val kafkaProps = Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBrokerConnectionString())
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            }
            return KafkaProducer<String, String>(kafkaProps)
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
                "kafka.brokers" to getBrokerConnectionString(),
                "kafka.localTest" to "true"
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