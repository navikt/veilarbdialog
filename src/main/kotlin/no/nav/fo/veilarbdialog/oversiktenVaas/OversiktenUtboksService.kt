package no.nav.fo.veilarbdialog.oversiktenVaas

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
open class OversiktenUtboksService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val oversiktenUtboksRepository: OversiktenUtboksRepository,
    private val oversiktenProducer: OversiktenProducer
) {

    @Scheduled(cron = "0 */5 * * * *") // Hvert 5. minutt
    @SchedulerLock(name = "oversikten_utboks_scheduledTask", lockAtMostFor = "PT3M")
    open fun sendUtgåtteVarslerTilOversikten() {
        val meldingerSomSkalSendes = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        meldingerSomSkalSendes.forEach { melding ->
            oversiktenProducer.sendMelding(melding.meldingKey.toString(), melding.meldingSomJson)
            // TODO: Marker sendt
        }
    }

    fun sendMeldingTilOversikten(gjeldendeEskaleringsvarsler: List<EskaleringsvarselEntity> ) {
        gjeldendeEskaleringsvarsler.forEach {
            val fnr =  aktorOppslagClient.hentFnr(AktorId(it.aktorId))
            val melding = utgåttVarselMelding(Operasjon.START, fnr)
            val sendingEntity = SendingEntity(meldingSomJson = JsonUtils.toJson(melding), fnr = fnr, kategori = melding.kategori, meldingKey = UUID.randomUUID())
            oversiktenUtboksRepository.lagreSending(sendingEntity)
        }
    }

    private fun utgåttVarselMelding(operasjon: Operasjon, fnr: Fnr) =
        OversiktenUtboksMelding(
            personID = fnr.get(),
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = operasjon,
            hendelse = Hendelse(
                beskrivelse = "Bruker har et utgått varsel",
                dato = LocalDateTime.now(),
                lenke = "url", // TODO: fiks ordentlig url
            )
        )

    data class OversiktenUtboksMelding(
        val personID: String,
        val avsender: String = "veilarbdialog",
        val kategori: Kategori,
        val operasjon: Operasjon,
        val hendelse: Hendelse
    )

    data class Hendelse (
        val beskrivelse: String,
        val dato: LocalDateTime,
        val lenke: String,
        val detaljer: String? = null,
    )

    enum class Kategori {
        UTGATT_VARSEL
    }

    enum class Operasjon {
        START,
        OPPDATER,
        STOPP
    }
}