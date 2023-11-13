package no.nav.dialogvarsler.plugins

import no.nav.dialogvarsler.varsler.WsTicketHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.dialogvarsler.varsler.EventType
import no.nav.dialogvarsler.varsler.TicketRequest
import no.nav.dialogvarsler.varsler.logger
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

fun Application.configureRouting(publishMessage: (message: NyDialogNotification) -> Long, pingRedis: PingRedis) {
    routing {
        route("/isAlive") {
            get {
                call.respond(HttpStatusCode.OK)
            }
        }
        route("/isReady") {
            get {
//                pingRedis()
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
                    try {
                        val subject = call.authentication.principal<TokenValidationContextPrincipal>()
                            ?.context?.anyValidClaims?.get()?.get("sub")?.toString() ?: throw IllegalArgumentException(
                            "No subject claim found")
                        val payload = call.receive<TicketRequest>()
                        val ticket = WsTicketHandler.generateTicket(subject, payload)
                        call.respondText(ticket)
                    } catch (e: CannotTransformContentToTypeException) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid payload")
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid auth")
                } catch (e: Throwable) {
                    call.respond(HttpStatusCode.InternalServerError, "Internal error")
                    logger.warn("Internal error", e)
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
