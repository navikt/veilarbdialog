package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

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
        val server: MockOAuth2Server by lazy {
            MockOAuth2Server()
                .also { it.start() }
        }

        @BeforeTest
        fun before() {
            EmbeddedKafkaSetup.start()
            server.start()
        }

        @AfterTest
        fun after() {
            EmbeddedKafkaSetup.stop()
            server.shutdown()
        }
    }
}
