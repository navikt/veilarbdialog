package no.nav.fo.veilarbdialog.minsidevarsler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType.BESKJED
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselService
import no.nav.fo.veilarbdialog.db.dao.DialogDAO
import no.nav.fo.veilarbdialog.domain.AvsenderType
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker
import no.nav.fo.veilarbdialog.service.DialogDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Service
open class SendGamleVarslerService(
    @Value("\${application.brukernotifikasjon.grace.periode.ms}")
    val brukernotifikasjonGracePeriode: Long, // 30 min
    @Value("\${application.brukernotifikasjon.henvendelse.maksalder.ms}")
    val brukernotifikasjonHenvendelseMaksAlder: Long, // 2 dager
    val dialogDAO: DialogDAO,
    val template: NamedParameterJdbcTemplate,
    val dialogDataService: DialogDataService,
    val minsideVarselService: MinsideVarselService,
    val aktorOppslagClient: AktorOppslagClient,
    val funksjonelleMetrikker: FunksjonelleMetrikker
) {

    private val log = LoggerFactory.getLogger(SendGamleVarslerService::class.java)

    // To minutter mellom hver kjøring
    @Scheduled(initialDelay = 60000, fixedDelay = 120000)
    @Transactional
    @SchedulerLock(name = "brukernotifikasjon_beskjed_kafka_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendBrukernotifikasjonerForUlesteDialoger() {
        val dialogIder: List<Long> = hentDialogerMedUlesteMeldingerEtterSisteVarsel(brukernotifikasjonGracePeriode, brukernotifikasjonHenvendelseMaksAlder);

        log.info("Varsler (beskjed): {} brukere", dialogIder.size)

        dialogIder.forEach { dialogId ->
            val dialogData = dialogDAO.hentDialog(dialogId)
            val fnr = aktorOppslagClient.hentFnr(AktorId.of(dialogData.aktorId))
            val oppfolgingsperiode = dialogData.oppfolgingsperiode

            val varselOppretting = DialogVarsel.varselOmNyMelding(
                dialogId,
                fnr,
                oppfolgingsperiode,
                dialogDataService.utledDialogLink(dialogId)
            )

            try {
                minsideVarselService.puttVarselIOutbox(varselOppretting, AktorId.of(dialogData.aktorId))
            } catch (e: BrukerKanIkkeVarslesException) {
                log.warn("Bruker kan ikke varsles.");
                funksjonelleMetrikker.nyBrukernotifikasjon(false, BESKJED);
            }
            funksjonelleMetrikker.nyBrukernotifikasjon(true, BESKJED);
        }
    }

    private fun hentDialogerMedUlesteMeldingerEtterSisteVarsel(graceMillis: Long, maxAgeMillis: Long): List<Long> {
        val minimumAlder = Date(System.currentTimeMillis() - graceMillis);
        val maksimumAlder = Date(System.currentTimeMillis() - maxAgeMillis);
        val params =  mapOf(
            "avsenderType" to AvsenderType.VEILEDER.name,
            "minimumAlder" to minimumAlder,
            "maksimumAlder" to maksimumAlder,
        )
        val sql = """
                select DISTINCT d.DIALOG_ID
                    from DIALOG d
                        left join HENVENDELSE h on h.DIALOG_ID = d.DIALOG_ID
                        left join VARSEL v on v.AKTOR_ID = d.AKTOR_ID
                        left join min_side_varsel_dialog_mapping m on m.dialog_id = d.DIALOG_ID
                    where h.AVSENDER_TYPE = :avsenderType -- Bare meldinger fra veileder
                        and m.dialogId is null -- Ikke meldinger på ny varsel-løsning
                        and (d.LEST_AV_BRUKER_TID is null or h.SENDT > d.LEST_AV_BRUKER_TID) -- Melding er nyere enn siste lest av bruker
                        and (v.SENDT is null or h.SENDT > v.SENDT) -- Meldinger sendt etter siste varsel bruker har fått
                        and h.SENDT < :minimumAlder -- Meldinger sendt for over 30 min siden
                        and h.SENDT > :maksimumAlder -- Melding ikke eldre enn 2 dager
                """
        return template.queryForList(sql, params, Long::class.java);
    }

}