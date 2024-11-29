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
    private val oversiktenMeldingMedMetadataRepository: OversiktenMeldingMedMetadataRepository,
    private val oversiktenProducer: OversiktenProducer
) {
    private val erProd = EnvironmentUtils.isProduction().orElse(false)

    @Scheduled(cron = "0 */5 * * * *") // Hvert 5. minutt
    @SchedulerLock(name = "oversikten_melding_med_metadata_scheduledTask", lockAtMostFor = "PT3M")
    open fun sendUsendteMeldingerTilOversikten() {
        val meldingerMedMetadata = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        meldingerMedMetadata.forEach { meldingMedMetadata ->
            oversiktenProducer.sendMelding(meldingMedMetadata.meldingKey.toString(), meldingMedMetadata.meldingSomJson)
            oversiktenMeldingMedMetadataRepository.markerSomSendt(meldingMedMetadata.meldingKey)
            meldingMedMetadata.fnr
        }
    }

    open fun sendStartMeldingOmUtgåttVarsel(eskaleringsvarsel: EskaleringsvarselEntity): MeldingKey {
        val fnr = aktorOppslagClient.hentFnr(AktorId(eskaleringsvarsel.aktorId))
        val melding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.START, erProd)
        val oversiktenMeldingMedMetadata = OversiktenMeldingMedMetadata(
            meldingSomJson = JsonUtils.toJson(melding),
            fnr = fnr,
            kategori = melding.kategori,
            meldingKey = UUID.randomUUID()
        )
        oversiktenMeldingMedMetadataRepository.lagre(oversiktenMeldingMedMetadata)
        return oversiktenMeldingMedMetadata.meldingKey
    }

    open fun sendStoppMeldingOmUtgåttVarsel(fnr: Fnr, meldingKeyStartMelding: UUID) {
        val opprinneligStartMelding = oversiktenMeldingMedMetadataRepository.hent(meldingKeyStartMelding, OversiktenMelding.Operasjon.START).let {
            check(it.size <= 1) { "Skal ikke kunne eksistere flere enn én startmeldinger" }
            it.first()
        }

        val sluttmelding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.STOPP, erProd)
        val oversiktenMeldingMedMetadata = OversiktenMeldingMedMetadata(
            meldingSomJson = JsonUtils.toJson(sluttmelding),
            fnr = fnr,
            kategori = sluttmelding.kategori,
            meldingKey = opprinneligStartMelding.meldingKey
        )

        try  {
            oversiktenProducer.sendMelding(oversiktenMeldingMedMetadata.meldingKey.toString(), oversiktenMeldingMedMetadata.meldingSomJson)
            val sendtMeldingMedMetadata = oversiktenMeldingMedMetadata.tilSendtMeldingMedMetadata()
            oversiktenMeldingMedMetadataRepository.lagre(sendtMeldingMedMetadata)
        } catch (e: Exception){
            oversiktenMeldingMedMetadataRepository.lagre(oversiktenMeldingMedMetadata)
        }
    }
}