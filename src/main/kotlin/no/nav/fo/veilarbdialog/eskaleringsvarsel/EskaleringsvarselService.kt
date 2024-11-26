package no.nav.fo.veilarbdialog.eskaleringsvarsel

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.domain.DialogStatus
import no.nav.fo.veilarbdialog.domain.Egenskap
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.domain.Person
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerIkkeUnderOppfolgingException
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException
import no.nav.fo.veilarbdialog.eventsLogger.BigQueryClient
import no.nav.fo.veilarbdialog.eventsLogger.EventType
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselService
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client
import no.nav.fo.veilarbdialog.oversiktenVaas.OversiktenService
import no.nav.fo.veilarbdialog.service.DialogDataService
import no.nav.poao.dab.spring_auth.IAuthService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Slf4j
@RequiredArgsConstructor
@Service
open class EskaleringsvarselService(
    private val minsideVarselService: MinsideVarselService,
    private val eskaleringsvarselRepository: EskaleringsvarselRepository,
    private val dialogDataService: DialogDataService,
    private val authService: IAuthService,
    private val aktorOppslagClient: AktorOppslagClient,
    private val oppfolgingClient: OppfolgingV2Client,
    private val sistePeriodeService: SistePeriodeService,
    private val funksjonelleMetrikker: FunksjonelleMetrikker,
    private val bigQueryClient: BigQueryClient,
    private val oversiktenService: OversiktenService
) {

    private val log = LoggerFactory.getLogger(EskaleringsvarselService::class.java)

    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "utgåtte_varsler_til_oversikten_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendUtgåtteVarslerTilOversikten() {
        val varselUtgåttEtterDager = 14
        val tidspunktUtgått = LocalDateTime.now().minusDays(varselUtgåttEtterDager.toLong())
        val varsler = eskaleringsvarselRepository.hentGjeldendeVarslerEldreEnn(tidspunktUtgått)
        oversiktenService.sendMeldingTilOversikten(varsler.toList())
    }

    @Transactional
    open fun start(stansVarsel: StartEskaleringDto): EskaleringsvarselEntity {
        if (hentGjeldende(stansVarsel.fnr).isPresent) {
            throw AktivEskaleringException("Brukeren har allerede en aktiv eskalering.")
        }

        if (!minsideVarselService.kanVarsles(stansVarsel.fnr)) {
            funksjonelleMetrikker.nyBrukernotifikasjon(false, BrukernotifikasjonsType.OPPGAVE)
            throw BrukerKanIkkeVarslesException()
        }

        if (!oppfolgingClient.erUnderOppfolging(stansVarsel.fnr)) {
            throw BrukerIkkeUnderOppfolgingException()
        }

        val nyMeldingDTO = NyMeldingDTO()
            .setTekst(stansVarsel.tekst)
            .setOverskrift(stansVarsel.overskrift)
            .setEgenskaper(java.util.List.of<Egenskap>(Egenskap.ESKALERINGSVARSEL))

        var dialogData = dialogDataService.opprettMelding(nyMeldingDTO, Person.fnr(stansVarsel.fnr.get()), false)

        val dialogStatus = DialogStatus.builder()
            .dialogId(dialogData.id)
            .venterPaSvar(true)
            .build()

        dialogData = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus)
        dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogData.id, true)
        dialogDataService.sendPaaKafka(dialogData.aktorId)
        dialogDataService.markerDialogSomLest(dialogData.id)

        val gjeldendeOppfolgingsperiodeId =
            sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(AktorId.of(dialogData.aktorId))

        val varselOmMuligStans = DialogVarsel.varselOmMuligStans(
            stansVarsel.fnr,
            gjeldendeOppfolgingsperiodeId,
            dialogDataService.utledDialogLink(dialogData.id)
        )

        minsideVarselService.puttVarselIOutbox(varselOmMuligStans, AktorId.of(dialogData.aktorId))
            ?: throw AktivEskaleringException("Det finnes allerede et oppgavevarsel for dialogId ${dialogData.id}.")

        val eskaleringsvarselEntity = eskaleringsvarselRepository.opprett(
            dialogData.id,
            varselOmMuligStans.varselId,
            dialogData.aktorId,
            authService.getLoggedInnUser().get(),
            stansVarsel.begrunnelse
        )

        funksjonelleMetrikker.nyBrukernotifikasjon(true, BrukernotifikasjonsType.OPPGAVE)
        log.info("Eskaleringsvarsel sendt forhåndsvarselId={}", varselOmMuligStans.varselId)
        log.info("Minside varsel opprettet i PENDING status {} forhåndsvarselId {}", varselOmMuligStans.varselId, eskaleringsvarselEntity.varselId)

        bigQueryClient.logEvent(eskaleringsvarselEntity, EventType.FORHAANDSVARSEL_OPPRETTET, stansVarsel.begrunnelseType)
        return eskaleringsvarselEntity
    }

    @Transactional
    open fun stop(stopVarselDto: StopEskaleringDto, avsluttetAv: NavIdent): Optional<EskaleringsvarselEntity> {
        val eskaleringsvarsel = hentGjeldende(stopVarselDto.fnr).orElse(null)
            ?: return Optional.empty() // Exit early with no errormessage?!

        if (stopVarselDto.skalSendeHenvendelse) {
            val nyHenvendelse = NyMeldingDTO()
                .setDialogId(eskaleringsvarsel.tilhorendeDialogId.toString())
                .setTekst(stopVarselDto.begrunnelse)
            dialogDataService.opprettMelding(nyHenvendelse, Person.fnr(stopVarselDto.fnr.get()), false)
        }

        eskaleringsvarselRepository.stop(eskaleringsvarsel.varselId, stopVarselDto.begrunnelse, avsluttetAv)
        minsideVarselService.inaktiverVarselForhåndsvarsel(eskaleringsvarsel)

        val eskaleringsvarselEntity = eskaleringsvarselRepository.hentVarsel(eskaleringsvarsel.varselId)
        eskaleringsvarselEntity.ifPresent { varsel ->
            bigQueryClient.logEvent(varsel, EventType.FORHAANDSVARSEL_INAKTIVERT)
        }
        return eskaleringsvarselEntity
    }

    @Transactional
    open fun stop(oppfolgingsperiode: UUID?): Boolean {
        return eskaleringsvarselRepository.stopPeriode(oppfolgingsperiode)
    }

    open fun hentGjeldende(fnr: Fnr?): Optional<EskaleringsvarselEntity?> {
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        return eskaleringsvarselRepository.hentGjeldende(aktorId)
    }

    open fun historikk(fnr: Fnr?): List<EskaleringsvarselEntity> {
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        return eskaleringsvarselRepository.hentHistorikk(aktorId)
    }
}
