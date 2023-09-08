package no.nav.dialogvarsler

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TicketRequest(
    val fnr: String,
)

object WsTicketHandler {
    suspend fun generateTicket(subject: String, payload: TicketRequest): ConnectionToken {
        val id = UUID.randomUUID().toString()
        WsConnectionHolder.wsConnectionTokenHolder[id] = ConnectionTicket(subject ,id, payload.fnr)
        return id
    }
}