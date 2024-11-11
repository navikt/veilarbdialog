package no.nav.fo.veilarbdialog.eskaleringsvarsel

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.fo.veilarbdialog.brukernotifikasjon.Brukernotifikasjon
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.domain.*
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerIkkeUnderOppfolgingException
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException
import no.nav.fo.veilarbdialog.eventsLogger.BigQueryClient
import no.nav.fo.veilarbdialog.eventsLogger.EventType
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client
import no.nav.fo.veilarbdialog.service.DialogDataService
import no.nav.poao.dab.spring_auth.IAuthService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Slf4j
@RequiredArgsConstructor
@Service
open class EskaleringsvarselService(
    private val brukernotifikasjonService: BrukernotifikasjonService,
    private val eskaleringsvarselRepository: EskaleringsvarselRepository,
    private val dialogDataService: DialogDataService,
    private val authService: IAuthService,
    private val aktorOppslagClient: AktorOppslagClient,
    private val oppfolgingClient: OppfolgingV2Client,
    private val sistePeriodeService: SistePeriodeService,
    private val funksjonelleMetrikker: FunksjonelleMetrikker,
    private val bigQueryClient: BigQueryClient,
) {

    private val log = LoggerFactory.getLogger(EskaleringsvarselService::class.java)

    @Transactional
    open fun start(stansVarsel: StartEskaleringDto): EskaleringsvarselEntity {
        if (hentGjeldende(stansVarsel.fnr).isPresent) {
            throw AktivEskaleringException("Brukeren har allerede en aktiv eskalering.")
        }

        if (!brukernotifikasjonService.kanVarsles(stansVarsel.fnr)) {
            funksjonelleMetrikker.nyBrukernotifikasjon(false, BrukernotifikasjonsType.OPPGAVE)
            throw BrukerKanIkkeVarslesException()
        }

        if (!oppfolgingClient.erUnderOppfolging(stansVarsel.fnr)) {
            throw BrukerIkkeUnderOppfolgingException()
        }

        val nyHenvendelseDTO = NyHenvendelseDTO()
            .setTekst(stansVarsel.tekst)
            .setOverskrift(stansVarsel.overskrift)
            .setEgenskaper(java.util.List.of<Egenskap>(Egenskap.ESKALERINGSVARSEL))

        var dialogData = dialogDataService.opprettHenvendelse(nyHenvendelseDTO, Person.fnr(stansVarsel.fnr.get()))

        val dialogStatus = DialogStatus.builder()
            .dialogId(dialogData.id)
            .venterPaSvar(true)
            .build()

        dialogData = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus)
        dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogData.id, true)
        dialogDataService.sendPaaKafka(dialogData.aktorId)
        dialogDataService.markerDialogSomLest(dialogData.id)

        val varselId = UUID.randomUUID()
        val gjeldendeOppfolgingsperiodeId =
            sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(AktorId.of(dialogData.aktorId))

        val brukernotifikasjon = Brukernotifikasjon(
            varselId,
            dialogData.id,
            stansVarsel.fnr,
            BrukernotifikasjonTekst.OPPGAVE_BRUKERNOTIFIKASJON_TEKST,
            gjeldendeOppfolgingsperiodeId,
            BrukernotifikasjonsType.OPPGAVE,
            dialogDataService.utledDialogLink(dialogData.id)
        )

        val brukernotifikasjonEntity =
            brukernotifikasjonService.bestillBrukernotifikasjon(brukernotifikasjon, AktorId.of(dialogData.aktorId))

        val eskaleringsvarselEntity = eskaleringsvarselRepository.opprett(
            dialogData.id,
            brukernotifikasjonEntity.id,
            dialogData.aktorId,
            authService.getLoggedInnUser().get(),
            stansVarsel.begrunnelse
        )

        funksjonelleMetrikker.nyBrukernotifikasjon(true, BrukernotifikasjonsType.OPPGAVE)
        log.info("Eskaleringsvarsel sendt varselId={}", varselId)

        bigQueryClient.logEvent(eskaleringsvarselEntity, EventType.FORHAANDSVARSEL_OPPRETTET, stansVarsel.begrunnelseType)
        return eskaleringsvarselEntity
    }

    @Transactional
    open fun stop(stopVarselDto: StopEskaleringDto, avsluttetAv: NavIdent): Optional<EskaleringsvarselEntity> {
        val eskaleringsvarsel = hentGjeldende(stopVarselDto.fnr).orElse(null)
            ?: return Optional.empty() // Exit early with no errormessage?!

        if (stopVarselDto.skalSendeHenvendelse) {
            val nyHenvendelse = NyHenvendelseDTO()
                .setDialogId(eskaleringsvarsel.tilhorendeDialogId.toString())
                .setTekst(stopVarselDto.begrunnelse)
            dialogDataService.opprettHenvendelse(nyHenvendelse, Person.fnr(stopVarselDto.fnr.get()))
        }

        eskaleringsvarselRepository.stop(eskaleringsvarsel.varselId, stopVarselDto.begrunnelse, avsluttetAv)
        brukernotifikasjonService.bestillDone(eskaleringsvarsel.tilhorendeBrukernotifikasjonId)

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
