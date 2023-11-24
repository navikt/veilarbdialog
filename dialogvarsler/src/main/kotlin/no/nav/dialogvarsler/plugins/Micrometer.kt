package no.nav.dialogvarsler.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Application.configureMicrometer() {
    install(MicrometerMetrics) {
        registry = Metrics.registry
    }
    routing {
        get("/metrics") {
            call.respond(Metrics.registry.scrape())
        }
    }
}

object Metrics {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}