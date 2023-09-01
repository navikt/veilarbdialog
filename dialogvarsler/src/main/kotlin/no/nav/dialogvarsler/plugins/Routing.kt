package no.nav.dialogvarsler.plugins

import no.nav.dialogvarsler.WsTicketHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/isAlive") {
            get {
                call.respond(HttpStatusCode.OK)
            }
        }
        route("/isReady") {
            get {
                call.respond(HttpStatusCode.OK)
            }
        }
        authenticate("AzureAD") {
            post("/ws-auth-ticket") {
                try {
                    val ticket = WsTicketHandler.generateTicket(this)
                    call.respondText(ticket)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid auth")
                } catch (e: Throwable) {
                    call.respond(HttpStatusCode.InternalServerError, "Internal error")
                    e.printStackTrace()
                }
            }
        }
    }
}