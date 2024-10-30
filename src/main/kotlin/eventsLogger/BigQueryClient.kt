package eventsLogger

import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

enum class EventType {
    FORHAANDSVARSEL_OPPRETTET,
    FORHAANDSVARSEL_INAKTIVERT
}

interface BigQueryClient {
    fun logEvent(eskaleringsvarselEntity: EskaleringsvarselEntity, eventType: EventType)
}

@Service
class BigQueryClientImplementation(@Value("\${project.gcp.projectId}") val projectId: String): BigQueryClient {
    val FORHAANSVARSEL_EVENTS = "FORHAANDSVARSEL_EVENTS"
    val DATASET_NAME = "aktivitet_metrikker"

    val log = LoggerFactory.getLogger(BigQueryClient::class.java)

    override fun logEvent(eskaleringsvarselEntity: EskaleringsvarselEntity, eventType: EventType) {
        val forhaandsvarselRow = mapOf(
            "id" to eskaleringsvarselEntity.varselId,
            "opprettet" to eskaleringsvarselEntity.opprettetDato,
            "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
            "event" to eventType.name
        )
    }
}
