package no.nav.fo.veilarbdialog.outbox

import no.nav.fo.veilarbdialog.db.jdbc.OutboxJdbcRepository
import org.springframework.stereotype.Repository

data class OutboxRecord(
    val id: Long,
    val topic: String,
    val key: String,
    val payload: String,
)

@Repository
open class OutboxDao(
    private val repository: OutboxJdbcRepository
) {
    open fun lagre(topic: String, key: String, payload: String) {
        repository.insert(topic, key, payload)
    }

    open fun hentUsendteMeldinger(event: OutboxMeldingLagretEvent? = null): List<OutboxRecord> {
        val rows = when (event) {
            null -> repository.findUsendte()
            else -> repository.findUsendteForKeyAndTopic(event.key, event.topic)
        }
        return rows.map { row ->
            OutboxRecord(
                id = row.id,
                topic = row.topic,
                key = row.messageKey,
                payload = row.payload,
            )
        }
    }

    open fun slett(id: Long) {
        repository.deleteById(id)
    }
}
