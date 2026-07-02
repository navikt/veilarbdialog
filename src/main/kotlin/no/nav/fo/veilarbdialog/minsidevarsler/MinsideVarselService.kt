package no.nav.fo.veilarbdialog.minsidevarsler

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.AVSLUTTET
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.PENDING
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.SENDT
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.SKAL_AVSLUTTES
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselRepository
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinsideVarselDao
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client
import no.nav.util.TeamLog.teamLog
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Slf4j
@RequiredArgsConstructor
open class MinsideVarselService(
    private val oppfolgingClient: OppfolgingV2Client,
    private val minsideVarselDao: MinsideVarselDao,
    private val minsideVarselProducer: MinsideVarselProducer,
    private val eskaleringsvarselRepository: EskaleringsvarselRepository,
) {
    private val log = LoggerFactory.getLogger(MinsideVarselService::class.java)

    open fun puttVarselIOutbox(varsel: DialogVarsel, aktorId: AktorId) {
        if (!kanVarsles(varsel.foedselsnummer)) {
            log.warn("Kan ikke varsle bruker: {}. Se årsak i TeamLog", aktorId.get())
            throw BrukerKanIkkeVarslesException()
        }

        if (varsel is DialogVarsel.VarselOmNyMelding && skalThrottles(varsel)) {
            log.info(
                "Minside varsel IKKE sendt ut pga nylig varsel i samme dialog {}",
                varsel.varselId
            )
            return
        }

        minsideVarselDao.opprettVarselIPendingStatus(varsel)
    }

    private fun skalThrottles(varsel: DialogVarsel.VarselOmNyMelding): Boolean {
        val eksisterendeVarsel = minsideVarselDao.getVarslerForDialog(varsel.dialogId)
        if (!eksisterendeVarsel.isEmpty()) {
            val halvtimeSiden = LocalDateTime.now().minusMinutes(30)
            // Hvis det er sendt eller skal sendes varsel for denne dialogen siste halvtimen, ikke opprett nytt varsel
            val uteståendeVarsler = eksisterendeVarsel.any { it ->
                (it.status == PENDING || it.status == SENDT)
                        && it.opprettet.isAfter(halvtimeSiden)
            }
            return uteståendeVarsler
        }
        return false
    }

    @Transactional
    open fun setSkalAvsluttesForVarslerIPeriode(oppfolgingsperiode: UUID) {
        minsideVarselDao.setSkalAvsluttesForVarslerIPeriode(oppfolgingsperiode)
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 5000)
    @SchedulerLock(name = "varsler_oppgave_kafka_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendPendingVarslerCron() {
        sendPendingVarslerCronImpl()
    }

    open public fun sendPendingVarslerCronImpl(): Int {
        val pendingVarsler = minsideVarselDao.hentPendingVarsler()
        pendingVarsler.forEach { varsel ->
            minsideVarselProducer.publiserVarselPåKafka(
                PendingVarsel(
                    varsel.varselId,
                    varsel.melding,
                    varsel.lenke,
                    varsel.type,
                    varsel.fnr,
                    varsel.skalBatches
                )
            )
            minsideVarselDao.updateStatus(varsel.varselId, SENDT)
        }

        return pendingVarsler.size
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 5000)
    @SchedulerLock(name = "varsel_inaktivering_kafka_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes() {
        val varslerIderSomSkalAvsluttes = minsideVarselDao.hentVarslerSomSkalAvsluttes()
        varslerIderSomSkalAvsluttes.stream().forEach { varselSomSkalAvsluttes ->
            minsideVarselProducer.publiserInaktiveringsMeldingPåKafka(varselSomSkalAvsluttes)
            minsideVarselDao.updateStatus(varselSomSkalAvsluttes, AVSLUTTET)
        }
    }

    open fun kanVarsles(fnr: Fnr): Boolean {
        val manuellStatusResponse = oppfolgingClient.hentManuellStatus(fnr)
        val erManuell = manuellStatusResponse
                .map { it.isErUnderManuellOppfolging }
                .orElse(true)
        val erReservertIKrr = manuellStatusResponse
                .map { it.krrStatus }
                .map { it.isErReservert }
                .orElse(true)

        val kanVarsles = !erManuell && !erReservertIKrr

        if (!kanVarsles) {
            teamLog.warn(
                "bruker med fnr: {} kan ikke varsles, statuser erManuell: {}, erReservertIKrr: {}}",
                fnr,
                erManuell,
                erReservertIKrr
            )
        }

        return kanVarsles
    }

    open fun inaktiverVarselForhåndsvarsel(eskaleringsvarselEntity: EskaleringsvarselEntity) {
        if (eskaleringsvarselEntity.tilhorendeVarselId != null) {
            minsideVarselDao.updateStatus(eskaleringsvarselEntity.tilhorendeVarselId, SKAL_AVSLUTTES)
        }
    }

    open fun inaktiverVarselForDialogEllerForhåndsvarsel(dialogId: Long, aktorId: AktorId) {
        // Sett tilhørende beskjeder til SKAL_AVSLUTTES
        minsideVarselDao.setDialogVarslerTilSkalAvsluttes(dialogId)

        // Hvis dialogen som blir lest tilhører et gjeldende varsel, sett tilhørende beskjeder til SKAL_AVSLUTTES
        eskaleringsvarselRepository.hentGjeldende(aktorId)
            .ifPresent { eskaleringsvarselEntity ->
                if (eskaleringsvarselEntity.tilhorendeDialogId() == dialogId) {
                    inaktiverVarselForhåndsvarsel(eskaleringsvarselEntity)
                }
            }
    }

    open fun finnesVarsel(varselId: MinSideVarselId): Boolean {
        return minsideVarselDao.finnesVarsel(varselId)
    }

    open fun setEksternVarselFeilet(varselId: MinSideVarselId) {
        minsideVarselDao.setEksternVarselFeilet(varselId)
    }

    open fun setEksternVarselKvitteringStatusOk(varselId: MinSideVarselId) {
        minsideVarselDao.setEksternVarselKvitteringStatusOk(varselId)
    }

    open fun setVarselstatusFerdigbehandlet(varselId: MinSideVarselId) {
        minsideVarselDao.updateStatus(varselId, AVSLUTTET)
    }

}