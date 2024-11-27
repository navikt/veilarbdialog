package no.nav.fo.veilarbdialog.oversiktenVaas

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

@Service
open class OversiktenService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val oversiktenUtboksRepository: OversiktenUtboksRepository,
    private val oversiktenProducer: OversiktenProducer
) {
    private val erProd = EnvironmentUtils.isProduction().orElse(false)

    @Scheduled(cron = "0 */5 * * * *") // Hvert 5. minutt
    @SchedulerLock(name = "oversikten_utboks_scheduledTask", lockAtMostFor = "PT3M")
    open fun sendUsendteMeldingerTilOversikten() {
        val meldingerSomSkalSendes = oversiktenUtboksRepository.hentAlleSomSkalSendes()
        meldingerSomSkalSendes.forEach { melding ->
            oversiktenProducer.sendMelding(melding.meldingKey.toString(), melding.meldingSomJson)
            oversiktenUtboksRepository.markerSomSendt(melding.meldingKey)
            melding.fnr
        }
    }

    open fun sendStartMeldingOmUtgåttVarsel(gjeldendeEskaleringsvarsler: List<EskaleringsvarselEntity>) {
        gjeldendeEskaleringsvarsler.forEach {
            val fnr = aktorOppslagClient.hentFnr(AktorId(it.aktorId))
            val melding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.START, erProd)
            val sendingEntity = SendingEntity(
                meldingSomJson = JsonUtils.toJson(melding),
                fnr = fnr,
                kategori = melding.kategori,
                meldingKey = UUID.randomUUID()
            )
            oversiktenUtboksRepository.lagreSending(sendingEntity)
        }
    }

    open fun sendStoppMeldingOmUtgåttVarsel(fnr: Fnr){
        // TODO: Hent start melding og gjenbruk key
        val melding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.STOPP, erProd)
        val sendingEntity = SendingEntity(
            meldingSomJson = JsonUtils.toJson(melding),
            fnr = fnr,
            kategori = melding.kategori,
            meldingKey = UUID.randomUUID()
        )
        try  {
            oversiktenProducer.sendMelding(sendingEntity.meldingKey.toString(), sendingEntity.meldingSomJson)
            oversiktenUtboksRepository.markerSomSendt(sendingEntity.meldingKey)
        }catch (e: Exception){
            oversiktenUtboksRepository.lagreSending(sendingEntity)
        }
    }
}