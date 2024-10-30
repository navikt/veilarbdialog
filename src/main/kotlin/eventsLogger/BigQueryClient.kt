package eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
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
class BigQueryClientImplementation(@Value("\${application.gcp.projectId}") val projectId: String): BigQueryClient {
    val FORHAANSVARSEL_EVENTS = "FORHAANDSVARSEL_EVENTS"
    val DATASET_NAME = "aktivitet_metrikker"
    val forhaandsvarselEventsTable = TableId.of(DATASET_NAME, FORHAANSVARSEL_EVENTS)

    fun TableId.insertRequest(row: Map<String, Any>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(this.javaClass)

    override fun logEvent(eskaleringsvarselEntity: EskaleringsvarselEntity, eventType: EventType) {
        runCatching {
            val forhaandsvarselRow = mapOf(
                "id" to eskaleringsvarselEntity.varselId,
                "opprettet" to eskaleringsvarselEntity.opprettetDato,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "event" to eventType.name
            )
            val insertRequest =forhaandsvarselEventsTable.insertRequest(forhaandsvarselRow)
            insertWhileToleratingErrors(insertRequest)
        }
            .onFailure {
                log.warn("Kunne ikke lage event i bigquery", it)
            }

    }

    private fun insertWhileToleratingErrors(insertRequest: InsertAllRequest) {
        runCatching {
            val response = bigQuery.insertAll(insertRequest)
            val errors = response.insertErrors
            if (errors.isNotEmpty()) {
                log.error("Error inserting bigquery rows", errors)
            }
        }.onFailure {
            log.error("BigQuery error", it)
        }
    }
}
