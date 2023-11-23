package no.nav.dialogvarsler.varsler

import kotlinx.serialization.Serializable
import java.lang.IllegalArgumentException
import java.util.*

@Serializable
data class TicketRequest(
    val fnr: String,
)

interface TicketStore {
    fun getTicket(ticket: ConnectionToken): ConnectionTicket?
    fun addTicket(token: ConnectionToken, ticket: ConnectionTicket)
    fun removeTicket(ticket: ConnectionToken): Unit
}

class WsTicketHandler(private val ticketStore: TicketStore) {
    // TODO: Only allow 1 ticket per sub
    // TODO: Make these expire after x-minutes
    private val wsConnectionTokenHolder = Collections.synchronizedMap<ConnectionToken, ConnectionTicket>(mutableMapOf())
    fun consumeTicket(ticket: ConnectionToken): ConnectionTicket {
        return ticketStore.getTicket(ticket)
            ?.also { ticketStore.removeTicket(ticket) }
            ?: throw IllegalArgumentException("Invalid or already used connection ticket")
    }
    fun generateTicket(subject: String, payload: TicketRequest): ConnectionToken {
        return UUID.randomUUID().toString()
            .also { ticketStore.addTicket(it, ConnectionTicket(subject ,it, payload.fnr)) }
    }
}