package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.brukernotifikasjon.Brukernotifikasjon;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.brukernotifikasjon.DoneInfo;
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselType;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerIkkeUnderOppfolgingException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.ZonedDateTime;
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
    private final VeilarboppfolgingClient veilarboppfolgingClient;
    private final SistePeriodeService sistePeriodeService;

    @Value("${application.dialog.url}")
    private String dialogUrl;

    @Transactional
    public EskaleringsvarselEntity start(Fnr fnr, String begrunnelse, String overskrift, String tekst) {

        if (hentGjeldende(fnr).isPresent()) {
            throw new AktivEskaleringException();
        }

        if (!brukernotifikasjonService.kanVarsles(fnr)) {
            throw new BrukerKanIkkeVarslesException();
        }

        if (!veilarboppfolgingClient.erUnderOppfolging(fnr)) {
            throw new BrukerIkkeUnderOppfolgingException();
        }

        NyHenvendelseDTO nyHenvendelseDTO = new NyHenvendelseDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift)
                .setEgenskaper(List.of(Egenskap.ESKALERINGSVARSEL));

        DialogData dialogData = dialogDataService.opprettHenvendelse(nyHenvendelseDTO, Person.fnr(fnr.get()));

        // TODO filtrer kontorsperre før retur

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
                tekst,
                gjeldendeOppfolgingsperiodeId,
                VarselType.ESKALERINGSVARSEL,
                overskrift, // Riktig?
                tekst, // Riktig?
                null, // TODO
                utledEskaleringsvarselLink(dialogData.getId()) // TODO
        );

        BrukernotifikasjonEntity brukernotifikasjonEntity = brukernotifikasjonService.sendBrukernotifikasjon(brukernotifikasjon);

        EskaleringsvarselEntity eskaleringsvarselEntity = eskaleringsvarselRepository.opprett(
                dialogData.getId(),
                brukernotifikasjonEntity.id(),
                dialogData.getAktorId(),
                authService.getIdent().orElseThrow(),
                begrunnelse
        );

        log.info("Eskaleringsvarsel sendt eventId={}", brukernotifikasjonId);

        return eskaleringsvarselEntity;
    }

    public void stop(Fnr fnr, String begrunnelse, boolean skalSendeHenvendelse, NavIdent avsluttetAv) {
        EskaleringsvarselEntity eskaleringsvarsel = hentGjeldende(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingen gjeldende eskaleringsvarsel"));

        if (skalSendeHenvendelse) {
            NyHenvendelseDTO nyHenvendelse = new NyHenvendelseDTO()
                    .setDialogId(Long.toString(eskaleringsvarsel.tilhorendeDialogId()))
                    .setTekst(begrunnelse);
            dialogDataService.opprettHenvendelse(nyHenvendelse, Person.fnr(fnr.get()));
        }
        eskaleringsvarselRepository.stop(eskaleringsvarsel.varselId(), begrunnelse, avsluttetAv);

        BrukernotifikasjonEntity brukernotifikasjonEntity = brukernotifikasjonService.hentBrukernotifikasjon(eskaleringsvarsel.tilhorendeBrukernotifikasjonId());

        DoneInfo doneInfo = DoneInfo.builder()
                .avsluttetTidspunkt(ZonedDateTime.now())
                .eventId(brukernotifikasjonEntity.eventId().toString())
                .oppfolgingsperiode(brukernotifikasjonEntity.oppfolgingsPeriodeId().toString())
                .build();

        brukernotifikasjonService.sendDone(fnr, doneInfo);
    }

    public Optional<EskaleringsvarselEntity> hentGjeldende(Fnr fnr) {
        AktorId aktorId = aktorOppslagClient.hentAktorId(fnr);

        return eskaleringsvarselRepository.hentGjeldende(aktorId);
    }

    public List<EskaleringsvarselEntity> historikk(Fnr fnr) {
        AktorId aktorId = aktorOppslagClient.hentAktorId(fnr);
        return eskaleringsvarselRepository.hentHistorikk(aktorId);
    }

    @SneakyThrows
    private URL utledEskaleringsvarselLink(long id) {
        return new URL(String.format("%s/%s", dialogUrl, id));
    }

}
