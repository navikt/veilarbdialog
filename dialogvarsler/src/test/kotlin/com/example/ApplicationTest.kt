package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import io.github.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment {
            doConfig()
        }
        application {
            module()
        }
        client.get("/isAlive").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("", bodyAsText())
        }

        client.post("/ws-auth-ticket").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }

        client.post("/ws-auth-ticket") {
            bearerAuth(server.issueToken().serialize())
            contentType(ContentType.Application.Json)
            setBody("""{ "fnr": "12345678910" }""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            UUID.fromString(this.bodyAsText())
        }

        val producer = getTestProducer()
        val messageToSend = "Hello, Kafka!"
        val record = ProducerRecord("test-topic","12345678910", messageToSend )
        producer.send(record).get()
    }

    private fun ApplicationEngineEnvironmentBuilder.doConfig(
            acceptedIssuer: String = "default",
            acceptedAudience: String = "default"
    ) {
        config = MapApplicationConfig(
                "no.nav.security.jwt.issuers.size" to "1",
                "no.nav.security.jwt.issuers.0.issuer_name" to acceptedIssuer,
                "no.nav.security.jwt.issuers.0.discoveryurl" to "${server.wellKnownUrl(acceptedIssuer)}",
                "no.nav.security.jwt.issuers.0.accepted_audience" to acceptedAudience
        )
    }

    companion object {
        var kafkaBrokerServer = EmbeddedKafkaSetup.start()

        val server: MockOAuth2Server by lazy {
            MockOAuth2Server()
                    .also { it.start() }
        }

        @BeforeTest
        fun before() {
            server.start()
        }

        @AfterTest
        fun after() {
            EmbeddedKafkaSetup.stop()
            server.shutdown()
        }

    }

    fun getTestProducer(): KafkaProducer<String, String> {
        var connectionString = kafkaBrokerServer?.broker()?.createBrokerInfo()?.broker()?.endPoints()?.find { true }?.get()?.connectionString()
                ?: throw Exception("Could not find brokerserver")
        val kafkaProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionString)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        }

        return KafkaProducer<String, String>(kafkaProps)

    }
}
