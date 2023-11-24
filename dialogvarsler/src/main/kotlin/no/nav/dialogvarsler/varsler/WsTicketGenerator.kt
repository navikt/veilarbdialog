package no.nav.dialogvarsler.varsler

import kotlinx.serialization.Serializable
import java.lang.IllegalArgumentException
import java.util.*

@Serializable
data class TicketRequest(
    val fnr: String,
)

sealed class ConnectionTicket {
    companion object {
        fun of(value: String): ConnectionTicket {
            return runCatching { UUID.fromString(value) }
                .getOrNull()?.let { ValidTicket(it.toString()) }
                ?: InvalidTicket
        }
    }
}
class ValidTicket(val value: String): ConnectionTicket()
data object InvalidTicket: ConnectionTicket()

interface TicketStore {
    fun getSubscription(ticket: ValidTicket): Subscription?
    fun addSubscription(token: ValidTicket, ticket: Subscription)
    fun removeSubscription(ticket: ValidTicket)
}

class WsTicketHandler(private val ticketStore: TicketStore) {
    // TODO: Only allow 1 ticket per sub
    // TODO: Make these expire after x-minutes
    fun consumeTicket(ticket: ValidTicket): Subscription {
        return ticketStore.getSubscription(ticket)
            ?.also { ticketStore.removeSubscription(ticket) }
            ?: throw IllegalArgumentException("Invalid or already used connection ticket")
    }
    fun generateTicket(subject: String, payload: TicketRequest): ValidTicket {
        return ValidTicket(UUID.randomUUID().toString())
            .also { ticketStore.addSubscription(it, Subscription(subject ,it.value, payload.fnr)) }
    }
}