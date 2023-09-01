package no.nav.dialogvarsler.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication() {
    val config = this.environment.config
    install(Authentication) {
        tokenValidationSupport(config = config, name = "AzureAD")
    }
}