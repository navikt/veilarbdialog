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
    private val oversiktenForsendingRepository: OversiktenForsendingRepository,
    private val oversiktenProducer: OversiktenProducer
) {
    private val erProd = EnvironmentUtils.isProduction().orElse(false)

    @Scheduled(cron = "0 */5 * * * *") // Hvert 5. minutt
    @SchedulerLock(name = "oversikten_forsending_scheduledTask", lockAtMostFor = "PT3M")
    open fun sendUsendteMeldingerTilOversikten() {
        val forsendingerSomSkalSendes = oversiktenForsendingRepository.hentAlleSomSkalSendes()
        forsendingerSomSkalSendes.forEach { forsending ->
            oversiktenProducer.sendMelding(forsending.uuid.toString(), forsending.meldingSomJson)
            oversiktenForsendingRepository.markerSomSendt(forsending.uuid)
            forsending.fnr
        }
    }

    open fun sendStartMeldingOmUtgåttVarsel(eskaleringsvarsel: EskaleringsvarselEntity): UUID {
        val fnr = aktorOppslagClient.hentFnr(AktorId(eskaleringsvarsel.aktorId))
        val melding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.START, erProd)
        val oversiktenForsendingEntity = OversiktenForsendingEntity(
            meldingSomJson = JsonUtils.toJson(melding),
            fnr = fnr,
            kategori = melding.kategori,
            uuid = UUID.randomUUID()
        )
        oversiktenForsendingRepository.lagreSending(oversiktenForsendingEntity)
        return oversiktenForsendingEntity.uuid
    }

    open fun sendStoppMeldingOmUtgåttVarsel(fnr: Fnr){
        // TODO: Hent start melding og gjenbruk key
        val melding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.STOPP, erProd)
        val oversiktenForsendingEntity = OversiktenForsendingEntity(
            meldingSomJson = JsonUtils.toJson(melding),
            fnr = fnr,
            kategori = melding.kategori,
            uuid = UUID.randomUUID()
        )
        try  {
            oversiktenProducer.sendMelding(oversiktenForsendingEntity.uuid.toString(), oversiktenForsendingEntity.meldingSomJson)
            oversiktenForsendingRepository.markerSomSendt(oversiktenForsendingEntity.uuid)
        }catch (e: Exception){
            oversiktenForsendingRepository.lagreSending(oversiktenForsendingEntity)
        }
    }
}