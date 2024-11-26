package no.nav.fo.veilarbdialog.oversiktenVaas

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

@Service
open class OversiktenUtboksService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val oversiktenUtboksRepository: OversiktenUtboksRepository,
    private val oversiktenProducer: OversiktenProducer
) {

    @Scheduled(cron = "0 */5 * * * *") // Hvert 5. minutt
    @SchedulerLock(name = "oversikten_utboks_scheduledTask", lockAtMostFor = "PT3M")
    open fun sendUsendteMeldingerTilOversikten() {
        val meldingerSomSkalSendes = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        meldingerSomSkalSendes.forEach { melding ->
            oversiktenProducer.sendMelding(melding.meldingKey.toString(), melding.meldingSomJson)
            oversiktenUtboksRepository.markerSomSendt(melding.meldingKey)
        }
    }

    open fun sendMeldingTilOversikten(gjeldendeEskaleringsvarsler: List<EskaleringsvarselEntity>) {
        gjeldendeEskaleringsvarsler.forEach {
            val fnr = aktorOppslagClient.hentFnr(AktorId(it.aktorId))
            val melding = OversiktenUtboksMelding.forUtgattVarsel(fnr.toString())
            val sendingEntity = SendingEntity(
                meldingSomJson = JsonUtils.toJson(melding),
                fnr = fnr,
                kategori = melding.kategori,
                meldingKey = UUID.randomUUID()
            )
            oversiktenUtboksRepository.lagreSending(sendingEntity)
        }
    }
}