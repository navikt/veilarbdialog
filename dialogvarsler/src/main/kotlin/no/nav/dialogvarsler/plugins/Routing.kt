package no.nav.dialogvarsler.plugins

import no.nav.dialogvarsler.WsTicketHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.dialogvarsler.EventType
import no.nav.dialogvarsler.TicketRequest
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import redis.clients.jedis.Jedis

fun Application.configureRouting(publishMessage: (message :NyDialogNotification) -> Long) {
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
            post("/notify-subscribers") {
                val dialogNotification = call.receive<NyDialogNotification>()
                publishMessage(dialogNotification)
                call.respond(status = HttpStatusCode.OK, message = "")
            }

            post("/ws-auth-ticket") {
                try {
                    // TODO: Add authorization(a2)
                    val subject = call.authentication.principal<TokenValidationContextPrincipal>()
                        ?.context?.anyValidClaims?.get()?.get("sub")?.toString() ?: throw IllegalArgumentException(
                        "No subject claim found")
                    val payload = call.receive<TicketRequest>()
                    val ticket = WsTicketHandler.generateTicket(subject, payload)
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

@Serializable
data class NyDialogNotification(
    val fnr: String,
    val eventType: EventType
)
