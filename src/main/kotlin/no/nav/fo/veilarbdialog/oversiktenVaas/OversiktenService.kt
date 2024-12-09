package no.nav.fo.veilarbdialog.oversiktenVaas

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
open class OversiktenService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val oversiktenMeldingMedMetadataRepository: OversiktenMeldingMedMetadataRepository,
    private val oversiktenProducer: OversiktenProducer
) {
    private val log = LoggerFactory.getLogger(OversiktenService::class.java)
    private val erProd = EnvironmentUtils.isProduction().orElse(false)

    @Scheduled(cron = "0 */1 * * * *") // Hvert minutt
    @SchedulerLock(name = "oversikten_melding_med_metadata_scheduledTask", lockAtMostFor = "PT3M")
    open fun sendUsendteMeldingerTilOversikten() {
        val meldingerMedMetadata = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        log.info("Sender ${meldingerMedMetadata.size} meldinger til oversikten")
        meldingerMedMetadata.forEach { meldingMedMetadata ->
            oversiktenMeldingMedMetadataRepository.markerSomSendt(meldingMedMetadata.id)
            oversiktenProducer.sendMelding(meldingMedMetadata.meldingKey.toString(), meldingMedMetadata.meldingSomJson)
            meldingMedMetadata.fnr
        }
    }

    open fun lagreStartMeldingOmUtgåttVarselIUtboks(eskaleringsvarsel: EskaleringsvarselEntity): MeldingKey {
        val fnr = aktorOppslagClient.hentFnr(AktorId(eskaleringsvarsel.aktorId))
        val utgåttTidspunkt = eskaleringsvarsel.opprettetDato.plusDays(10).toLocalDateTime()
        val melding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.START, utgåttTidspunkt, erProd)
        val oversiktenMeldingMedMetadata = OversiktenMeldingMedMetadata(
            meldingSomJson = JsonUtils.toJson(melding),
            fnr = fnr,
            kategori = melding.kategori,
            meldingKey = UUID.randomUUID(),
            operasjon = melding.operasjon,
        )
        oversiktenMeldingMedMetadataRepository.lagre(oversiktenMeldingMedMetadata)
        return oversiktenMeldingMedMetadata.meldingKey
    }

    open fun lagreStoppMeldingOmUtgåttVarselIUtboks(fnr: Fnr, meldingKey: UUID) {
        val stoppMelding = OversiktenMelding.forUtgattVarsel(fnr.toString(), OversiktenMelding.Operasjon.STOPP, LocalDateTime.now(), erProd)
        val oversiktenMeldingMedMetadata = OversiktenMeldingMedMetadata(
            meldingSomJson = JsonUtils.toJson(stoppMelding),
            fnr = fnr,
            kategori = stoppMelding.kategori,
            meldingKey = meldingKey,
            operasjon = stoppMelding.operasjon,
        )
        oversiktenMeldingMedMetadataRepository.lagre(oversiktenMeldingMedMetadata)
    }
}