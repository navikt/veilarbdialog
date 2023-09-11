package no.nav.dialogvarsler.varsler

import kotlinx.serialization.Serializable
import java.lang.IllegalArgumentException
import java.util.*

@Serializable
data class TicketRequest(
    val fnr: String,
)

object WsTicketHandler {
    private val wsConnectionTokenHolder = Collections.synchronizedMap<ConnectionToken, ConnectionTicket>(mutableMapOf())
    fun consumeTicket(ticket: String): ConnectionTicket {
        if (ticket in wsConnectionTokenHolder) {
            return wsConnectionTokenHolder.remove(ticket) ?:
                throw IllegalArgumentException("Invalid or already used connection ticket")
        } else {
            throw IllegalArgumentException("Invalid or already used connection ticket")
        }
    }
    fun generateTicket(subject: String, payload: TicketRequest): ConnectionToken {
        val id = UUID.randomUUID().toString()
        wsConnectionTokenHolder[id] = ConnectionTicket(subject ,id, payload.fnr)
        return id
    }
}