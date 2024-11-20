package no.nav.fo.veilarbdialog.minsidevarsler

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.AVSLUTTET
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.PENDING
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.SENDT
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType.BESKJED
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.ManuellStatusV2DTO
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselRepository
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException
import no.nav.fo.veilarbdialog.minsidevarsler.dto.DialogVarselStatus
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinsideVarselDao
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Function

@Service
@Slf4j
@RequiredArgsConstructor
open class MinsideVarselService(
    private val oppfolgingClient: OppfolgingV2Client,
    private val brukernotifikasjonRepository: BrukernotifikasjonRepository,
    private val minsideVarselDao: MinsideVarselDao,
    private val minsideVarselProducer: MinsideVarselProducer,
    private val eskaleringsvarselRepository: EskaleringsvarselRepository,
) {
    private val secureLogs: Logger = LoggerFactory.getLogger("SecureLog")
    private val log = LoggerFactory.getLogger(MinsideVarselService::class.java)

    open fun puttVarselIOutbox(varsel: DialogVarsel, aktorId: AktorId) {
        if (!kanVarsles(varsel.foedselsnummer)) {
            log.warn("Kan ikke varsle bruker: {}. Se årsak i SecureLog", aktorId.get())
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
        log.info("Minside varsel opprettet i PENDING status {}", varsel.varselId)
    }

    private fun skalThrottles(varsel: DialogVarsel.VarselOmNyMelding): Boolean {
        val eksisterendeVarsel =
            brukernotifikasjonRepository.hentBrukernotifikasjonForDialogId(varsel.dialogId, varsel.type)
        val eksisterendeNyTypeVarsel = minsideVarselDao.getVarslerForDialog(varsel.dialogId)
        if (!eksisterendeVarsel.isEmpty() || !eksisterendeNyTypeVarsel.isEmpty()) {
            val halvtimeSiden = LocalDateTime.now().minusMinutes(30)
            // Hvis det er sendt eller skal sendes varsel for denne dialogen siste halvtimen, ikke opprett nytt varsel
            val uteståendeVarslerGammel = eksisterendeVarsel.any { it ->
                (it.status == PENDING || it.status == SENDT)
                        && it.opprettet.isAfter(halvtimeSiden)
            }
            val uteståendeVarslerNy = eksisterendeNyTypeVarsel.any { it ->
                (it.status == PENDING || it.status == SENDT)
                        && it.opprettet.isAfter(halvtimeSiden)
            }
            return uteståendeVarslerGammel || uteståendeVarslerNy
        }
        return false
    }

    @Transactional
    open fun setSkalAvsluttesForVarslerIPeriode(oppfolgingsperiode: UUID) {
        brukernotifikasjonRepository.setSkalAvsluttesForVarslerIPeriode(oppfolgingsperiode)
        minsideVarselDao.setSkalAvsluttesForVarslerIPeriode(oppfolgingsperiode)
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 5000)
    @SchedulerLock(name = "varsler_oppgave_kafka_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendPendingVarslerCron() {
        sendPendingVarslerCronImpl()
    }

    open public fun sendPendingVarslerCronImpl(): Int {
        val pendingBrukernotifikasjoner = brukernotifikasjonRepository.hentPendingVarsler()
        pendingBrukernotifikasjoner.forEach { brukernotifikasjonEntity ->
            minsideVarselProducer.publiserVarselPåKafka(
                PendingVarsel(
                    brukernotifikasjonEntity.varselId,
                    brukernotifikasjonEntity.melding,
                    brukernotifikasjonEntity.lenke,
                    brukernotifikasjonEntity.type,
                    brukernotifikasjonEntity.fnr,
                    brukernotifikasjonEntity.skalBatches
                )
            )
            brukernotifikasjonRepository.updateStatus(
                brukernotifikasjonEntity.varselId, SENDT
            )
        }
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

        return pendingBrukernotifikasjoner.size + pendingVarsler.size
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 5000)
    @SchedulerLock(name = "varsel_inaktivering_kafka_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendInktiveringPåKafkaPåVarslerSomSkalAvsluttes() {
        val skalAvsluttesNotifikasjoner = brukernotifikasjonRepository.hentVarslerSomSkalAvsluttes()
        skalAvsluttesNotifikasjoner.stream().forEach { varselSomSkalAvsluttes ->
            minsideVarselProducer.publiserInaktiveringsMeldingPåKafka(
                varselSomSkalAvsluttes.varselId
            )
            brukernotifikasjonRepository.updateStatus(
                varselSomSkalAvsluttes.varselId,
                AVSLUTTET
            )
        }

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
            secureLogs.warn(
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
            // Ny tabell
            minsideVarselDao.updateStatus(eskaleringsvarselEntity.tilhorendeVarselId, SKAL_AVSLUTTES)
        } else {
            // Gammel tabell
            brukernotifikasjonRepository.hentBrukernotifikasjon(eskaleringsvarselEntity.tilhorendeBrukernotifikasjonId())
                .map { it.varselId }
                .ifPresent { varselId -> brukernotifikasjonRepository.updateStatus(varselId, SKAL_AVSLUTTES) }
        }
    }

    open fun inaktiverVarselForDialogEllerForhåndsvarsel(dialogId: Long, aktorId: AktorId) {
        // Sett tilhørende beskjeder til SKAL_AVSLUTTES
        // Gammel tabell
        brukernotifikasjonRepository
            .hentBrukernotifikasjonForDialogId(dialogId, BESKJED)
            .forEach { varsel -> brukernotifikasjonRepository.updateStatus(varsel.varselId, SKAL_AVSLUTTES) }
        // Ny tabell
        minsideVarselDao.setDialogVarslerTilSkalAvsluttes(dialogId)

        // Hvis dialogen som blir lest tilhører et gjeldende varsel, sett tilhørende beskjeder til SKAL_AVSLUTTES
        eskaleringsvarselRepository.hentGjeldende(aktorId)
            .ifPresent { eskaleringsvarselEntity ->
                if (eskaleringsvarselEntity.tilhorendeDialogId() == dialogId) {
                    inaktiverVarselForhåndsvarsel(eskaleringsvarselEntity)
                }
            }
    }

    open fun finnesBrukernotifikasjon(varselId: MinSideVarselId): Boolean {
        val finnesIGammelTabell =brukernotifikasjonRepository.finnesBrukernotifikasjon(varselId)
        val finnesINyTabell = minsideVarselDao.finnesBrukernotifikasjon(varselId)
        return finnesIGammelTabell && finnesINyTabell
    }

    open fun setEksternVarselFeilet(varselId: MinSideVarselId) {
        // Gammel tabell
        brukernotifikasjonRepository.setEksternVarselFeilet(varselId)
        // Ny tabell
        minsideVarselDao.setEksternVarselFeilet(varselId)
    }

    open fun setEksternVarselSendtOk(varselId: MinSideVarselId) {
        // Gammel tabell
        brukernotifikasjonRepository.setEksternVarselSendtOk(varselId)
        // Ny tabell
        minsideVarselDao.setEksternVarselSendtOk(varselId)
    }

    open fun setEksternVarselAvsluttet(varselId: MinSideVarselId) {
        // Gammel tabell
        brukernotifikasjonRepository.updateStatus(varselId, AVSLUTTET)
        // Ny tabell
        minsideVarselDao.updateStatus(varselId, AVSLUTTET)
    }
}