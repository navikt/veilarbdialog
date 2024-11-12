package no.nav.fo.veilarbdialog.minsidevarsler

import lombok.RequiredArgsConstructor
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.db.dao.DialogDAO
import no.nav.fo.veilarbdialog.db.dao.VarselDAO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker
import no.nav.fo.veilarbdialog.service.DialogDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@RequiredArgsConstructor
open class ScheduleSendBrukernotifikasjonerForUlesteDialoger(
    private val dialogDAO: DialogDAO,
    private val varselDAO: VarselDAO,
    private val dialogDataService: DialogDataService,
    private val funksjonelleMetrikker: FunksjonelleMetrikker,
    private val brukernotifikasjonService: BrukernotifikasjonService,
    private val aktorOppslagClient: AktorOppslagClient,
    @Value("\${application.brukernotifikasjon.grace.periode.ms}")
    private val varselGracePeriode: Long, // 30 min
    @Value("\${application.brukernotifikasjon.henvendelse.maksalder.ms}")
    private val varselHenvendelseMaksAlder: Long, // 2 dager
) {
    private val log = LoggerFactory.getLogger(ScheduleSendBrukernotifikasjonerForUlesteDialoger::class.java)

    // To minutter mellom hver kjøring
    @Scheduled(initialDelay = 60000, fixedDelay = 120000)
    @Transactional
    @SchedulerLock(name = "brukernotifikasjon_beskjed_kafka_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendBrukernotifikasjonerForUlesteDialoger() {
        val dialogIder = varselDAO.hentDialogerMedUlesteMeldingerEtterSisteVarsel(
            varselGracePeriode,
            varselHenvendelseMaksAlder
        )

        log.info("Varsler (beskjed): {} brukere", dialogIder.size)

        dialogIder.forEach { dialogId ->
            val dialogData = dialogDAO.hentDialog(dialogId)
            val fnr = aktorOppslagClient.hentFnr(AktorId.of(dialogData.aktorId))

            val oppfolgingsperiode = dialogData.oppfolgingsperiode

            val varselOmUlestMelding = DialogVarsel.varselOmNyMelding(
                dialogData.id,
                fnr,
                oppfolgingsperiode,
                dialogDataService.utledDialogLink(dialogId)
            )

            try {
                brukernotifikasjonService.bestillVarsel(
                    varselOmUlestMelding,
                    AktorId.of(dialogData.aktorId)
                )
            } catch (e: BrukerKanIkkeVarslesException) {
                log.warn("Bruker kan ikke varsles.")
                funksjonelleMetrikker.nyBrukernotifikasjon(false, BrukernotifikasjonsType.BESKJED)
            }
            funksjonelleMetrikker.nyBrukernotifikasjon(true, BrukernotifikasjonsType.BESKJED)
        }

    }
}
