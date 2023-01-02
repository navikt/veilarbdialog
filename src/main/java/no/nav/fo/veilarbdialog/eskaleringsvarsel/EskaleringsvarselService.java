package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.brukernotifikasjon.Brukernotifikasjon;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.OppfolgingClient;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerIkkeUnderOppfolgingException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class EskaleringsvarselService {

    private final BrukernotifikasjonService brukernotifikasjonService;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;
    private final DialogDataService dialogDataService;
    private final AuthService authService;
    private final AktorOppslagClient aktorOppslagClient;
    private final OppfolgingClient oppfolgingClient;
    private final SistePeriodeService sistePeriodeService;
    private final FunksjonelleMetrikker funksjonelleMetrikker;

    @Transactional
    public EskaleringsvarselEntity start(Fnr fnr, String begrunnelse, String overskrift, String tekst) {

        if (hentGjeldende(fnr).isPresent()) {
            throw new AktivEskaleringException("Brukeren har allerede en aktiv eskalering.");
        }

        if (!brukernotifikasjonService.kanVarsles(fnr)) {
            funksjonelleMetrikker.nyBrukernotifikasjon(false, BrukernotifikasjonsType.OPPGAVE);
            throw new BrukerKanIkkeVarslesException();
        }

        if (!oppfolgingClient.erUnderOppfolging(fnr)) {
            throw new BrukerIkkeUnderOppfolgingException();
        }

        NyHenvendelseDTO nyHenvendelseDTO = new NyHenvendelseDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift)
                .setEgenskaper(List.of(Egenskap.ESKALERINGSVARSEL));

        DialogData dialogData = dialogDataService.opprettHenvendelse(nyHenvendelseDTO, Person.fnr(fnr.get()));

        var dialogStatus = DialogStatus.builder()
                .dialogId(dialogData.getId())
                .venterPaSvar(true)
                .build();

        dialogData = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);

        dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogData.getId(), true);

        dialogDataService.sendPaaKafka(dialogData.getAktorId());

        dialogDataService.markerDialogSomLest(dialogData.getId());


        UUID brukernotifikasjonId = UUID.randomUUID();

        UUID gjeldendeOppfolgingsperiodeId = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(AktorId.of(dialogData.getAktorId()));

        Brukernotifikasjon brukernotifikasjon = new Brukernotifikasjon(
                brukernotifikasjonId,
                dialogData.getId(),
                fnr,
                BrukernotifikasjonTekst.OPPGAVE_BRUKERNOTIFIKASJON_TEKST,
                gjeldendeOppfolgingsperiodeId,
                BrukernotifikasjonsType.OPPGAVE,
                dialogDataService.utledDialogLink(dialogData.getId())
        );


        BrukernotifikasjonEntity brukernotifikasjonEntity = brukernotifikasjonService.bestillBrukernotifikasjon(brukernotifikasjon, AktorId.of(dialogData.getAktorId()));

        EskaleringsvarselEntity eskaleringsvarselEntity = eskaleringsvarselRepository.opprett(
                dialogData.getId(),
                brukernotifikasjonEntity.id(),
                dialogData.getAktorId(),
                authService.getIdent().orElseThrow(),
                begrunnelse
        );

        funksjonelleMetrikker.nyBrukernotifikasjon(true, BrukernotifikasjonsType.OPPGAVE);
        log.info("Eskaleringsvarsel sendt eventId={}", brukernotifikasjonId);

        return eskaleringsvarselEntity;
    }

    @Transactional
    public Optional<EskaleringsvarselEntity> stop(Fnr fnr, String begrunnelse, boolean skalSendeHenvendelse, NavIdent avsluttetAv) {
        EskaleringsvarselEntity eskaleringsvarsel = hentGjeldende(fnr).orElse(null);

        if (eskaleringsvarsel == null) {
            return Optional.empty();
        }

        if (skalSendeHenvendelse) {
            NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                    .setDialogId(Long.toString(eskaleringsvarsel.tilhorendeDialogId()))
                    .setTekst(begrunnelse);
            dialogDataService.opprettHenvendelse(nyHenvendelse, Person.fnr(fnr.get()));
        }
        eskaleringsvarselRepository.stop(eskaleringsvarsel.varselId(), begrunnelse, avsluttetAv);

        brukernotifikasjonService.bestillDone(eskaleringsvarsel.tilhorendeBrukernotifikasjonId());

        return eskaleringsvarselRepository.hentVarsel(eskaleringsvarsel.varselId());
    }

    @Transactional
    public boolean stop(UUID oppfolgingsperiode) {
        return eskaleringsvarselRepository.stopPeriode(oppfolgingsperiode);
    }

    public Optional<EskaleringsvarselEntity> hentGjeldende(Fnr fnr) {
        AktorId aktorId = aktorOppslagClient.hentAktorId(fnr);

        return eskaleringsvarselRepository.hentGjeldende(aktorId);
    }


    public List<EskaleringsvarselEntity> historikk(Fnr fnr) {
        AktorId aktorId = aktorOppslagClient.hentAktorId(fnr);
        return eskaleringsvarselRepository.hentHistorikk(aktorId);
    }

}
