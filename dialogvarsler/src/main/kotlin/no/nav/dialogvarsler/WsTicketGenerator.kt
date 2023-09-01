package no.nav.dialogvarsler

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.lang.IllegalArgumentException
import java.util.UUID

@Serializable
data class TicketRequest(
    val fnr: String,
)

object WsTicketHandler {
    suspend fun generateTicket(pipelineContext: PipelineContext<Unit, ApplicationCall>): ConnectionToken {
        // TODO: Add authorization(a2)
        val subject = pipelineContext.call.authentication.principal<TokenValidationContextPrincipal>()
            ?.context?.anyValidClaims?.get()?.get("sub")?.toString() ?: throw IllegalArgumentException("No subject claim found")
        val payload = pipelineContext.call.receive<TicketRequest>()
        val id = UUID.randomUUID().toString()
        WsConnectionHolder.wsConnectionTokenHolder[id] = ConnectionTicket(subject ,id, payload.fnr)
        return id
    }
}