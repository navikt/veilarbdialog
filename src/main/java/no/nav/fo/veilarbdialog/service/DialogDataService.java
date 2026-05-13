package no.nav.fo.veilarbdialog.service;

import io.getunleash.Unleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.fo.veilarbdialog.dialog.*;
import no.nav.fo.veilarbdialog.dialog.exceptions.FantIkkeDialogTrådException;
import no.nav.fo.veilarbdialog.dialog.opprett.*;
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselService;
import no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService;
import no.nav.fo.veilarbdialog.service.exceptions.NyHenvendelsePåHistoriskDialogException;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.util.*;

import static no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV;
import static no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DialogDataService {

    private final AktorOppslagClient aktorOppslagClient;
    private final DialogVarslerClient dialogVarslerClient;
    private final DialogDAO dialogDAO;
    private final DialogStatusService dialogStatusService;
    private final DataVarehusDAO dataVarehusDAO;
    private final KvpService kvpService;
    private final KafkaProducerService kafkaProducerService;
    private final IAuthService auth;
    private final KladdService kladdService;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final SistePeriodeService sistePeriodeService;
    private final Unleash unleash;

    private final MinsideVarselService minsideVarselService;

    @Value("${application.dialog.url}")
    private String dialogUrl;

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(Person person) {
        AktorId aktorId = hentAktoerIdForPerson(person);
        return dialogDAO.hentDialogerForAktorId(aktorId.get());
    }

    public Date hentSistOppdatertForBruker(Person person, Id endretAvId) {
        Id endretAv = auth.erEksternBruker() ? hentAktoerIdForPerson(endretAvId) : endretAvId;
        AktorId bruker = hentAktoerIdForPerson(person);
        return dataVarehusDAO.hentSisteEndringSomIkkeErDine(bruker.get(), endretAv.get());
    }

    public DialogData getOrCreateDialogTråd(NyDialogEllerMelding henvendelseData, Optional<DialogData> dialogData) {
        if (henvendelseData instanceof NyDialog opprettDialogData) {
            return opprettDialog(opprettDialogData);
        } else if (dialogData.isPresent()) {
            return dialogData.get();
        } else {
            throw new FantIkkeDialogTrådException(((NyMelding) henvendelseData).getDialogId() + "");
        }
    }

    private @NotNull DialogVarsel toVarselMelding(DialogData dialog, Fnr fnr) {
        return DialogVarsel.Companion.varselOmNyMelding(
                dialog.getId(),
                fnr,
                dialog.getOppfolgingsperiode(),
                utledDialogLink(dialog.getId())
        );
    }

    /**
     * Varsler skal kun sendes ut når det er Nav som sender ut meldingen.
     * Eskaleringsvarsel-service sender ut varsel selv og setter skalSendeMinsideVarsel til false for å unngå å sende ut felre varsler.
     * */
    @Transactional
    public DialogData opprettMelding(NyMeldingDTO nyDialogEllerMeldingDto, Person bruker, Boolean skalSendeMinsideVarsel) {
        var aktivitetsId = AktivitetId.of(nyDialogEllerMeldingDto.getAktivitetId());
        var aktorId = hentAktoerIdForPerson(bruker);
        var fnr = hentFnrForPerson(bruker);
        if (skalSendeMinsideVarsel && !minsideVarselService.kanVarsles(fnr)) {
            throw new ResponseStatusException(CONFLICT, "Bruker kan ikke varsles.");
        }

        var maybeDialog = hentDialog(nyDialogEllerMeldingDto.getDialogId(), aktivitetsId, aktorId);
        if (maybeDialog.isEmpty() && nyDialogEllerMeldingDto.getDialogId() != null) throw new FantIkkeDialogTrådException(nyDialogEllerMeldingDto.getDialogId());
        if(maybeDialog.isPresent() && maybeDialog.get().isHistorisk()) throw new NyHenvendelsePåHistoriskDialogException();

        var henvendelseData = DialogDomainMapper.tilNyMeldingEllerDialog(
                nyDialogEllerMeldingDto,
                maybeDialog.isPresent(),
                fnr, aktorId,
                auth.getLoggedInnUser().get(),
                auth.erEksternBruker() ? AvsenderType.BRUKER : AvsenderType.VEILEDER
        );
        var dialog = getOrCreateDialogTråd(henvendelseData, maybeDialog);

        slettKladd(nyDialogEllerMeldingDto.getDialogId(), nyDialogEllerMeldingDto.getAktivitetId(), bruker);
        var opprettetMelding = opprettMeldingForDialog(henvendelseData, dialog.getId());
        dialogStatusService.oppdaterDialogTrådStatuserForNyMelding(dialog, opprettetMelding);
        dialog = markerDialogSomLest(dialog.getId());
        sendUtMinsideVarselHvisDetSkalSendesUt(dialog, fnr, aktorId, skalSendeMinsideVarsel);
        varsleWebsocketLyttereHvisToggletPaa(fnr);

        if (henvendelseData instanceof NyDialogFraVeileder nyDialog) {
            oppdaterFerdigbehandletTidspunkt(dialog.getId(), !nyDialog.getVenterPaaSvarFraNav());
            dialog = oppdaterVentePaSvarTidspunkt(dialog.getId(), nyDialog.getVenterPaaSvarFraBruker());
        }
        if (henvendelseData instanceof NyDialogFraBruker nyDialog) {
            dialog =oppdaterFerdigbehandletTidspunkt(dialog.getId(), !nyDialog.getVenterPaaSvarFraNav());
        }

        // Vent med å sende på kafka til alle statuser er oppdatert
        sendPaaKafka(aktorId.get());

        return dialog;
    }

    public DialogData opprettEskaleringsvarselDialogOgMelding(NyEskaleringsVarselDialog data) {
        var dialog = opprettDialog(data);
        var opprettetMelding = opprettMeldingForDialog(data, dialog.getId());
        dialogStatusService.oppdaterDialogTrådStatuserForNyMelding(dialog, opprettetMelding);
        varsleWebsocketLyttereHvisToggletPaa(data.getFnr());
        oppdaterVentePaSvarTidspunkt(dialog.getId(), true);
        oppdaterFerdigbehandletTidspunkt(dialog.getId(),true);
        dialog = markerDialogSomLest(dialog.getId());
        // Vent med å sende på kafka til alle statuser er oppdatert
        sendPaaKafka(data.getAktorId().get());
        return dialog;
    }

    private void sendUtMinsideVarselHvisDetSkalSendesUt(DialogData dialog, Fnr fnr, AktorId aktorId, Boolean skalSendeMinsideVarsel) {
        if (skalSendeMinsideVarsel) {
            var varselOmNyMelding = toVarselMelding(dialog, fnr);
            minsideVarselService.puttVarselIOutbox(varselOmNyMelding, aktorId);
            log.info("Minside varsel opprettet i PENDING status {} dialogId {}", varselOmNyMelding.getVarselId(), dialog.getId());
        }
    }

    private void varsleWebsocketLyttereHvisToggletPaa(Fnr fnr) {
        if (unleash.isEnabled("veilarbdialog.dialogvarsling")) {
            var eventType = auth.erEksternBruker() ? NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV : NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER;
            dialogVarslerClient.varsleLyttere(fnr, eventType);
        }
    }

    public DialogData markerDialogSomLest(long dialogId) {
        if (auth.erEksternBruker()) {
            return markerDialogSomLestAvBruker(dialogId);
        }
        return markerDialogSomLestAvVeileder(dialogId);

    }

    public DialogData oppdaterFerdigbehandletTidspunkt(long dialogId, boolean ferdigBehandlet) {
        var dialogData = hentDialogSomKanOppdateres(dialogId);
        return dialogStatusService.oppdaterVenterPaNavSiden(dialogData, ferdigBehandlet);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(long dialogId,  boolean venterPåSvarFraBruker) {
        var dialogData = hentDialogSomKanOppdateres(dialogId);
        return dialogStatusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, venterPåSvarFraBruker);
    }

    private HenvendelseData opprettMeldingForDialog(NyDialogEllerMelding dialogData, Long dialogId) {
        NyMelding nyMelding = dialogData instanceof NyMelding m ? m : DialogDomainMapper.nyDialogTilNyMelding((NyDialog) dialogData, dialogId);
        var viktigMelding = dialogData instanceof NyEskaleringsVarselDialog;

        return dialogDAO.opprettHenvendelse(HenvendelseData.builder()
                .dialogId(dialogId)
                .avsenderId(dialogData.getAvsenderId())
                .viktig(viktigMelding)
                .avsenderType(dialogData.getAvsenderType())
                .tekst(dialogData.getTekst())
                .kontorsperreEnhetId(kvpService.kontorsperreEnhetId(nyMelding.getFnr()))
                .sendt(new Date())
                .build());
    }

    private DialogData hentDialogSomKanOppdateres(long id) {
        var dialogData = hentDialogUtenTilgangsSjekk(id);
        if (dialogData.isHistorisk()) {
            throw new ResponseStatusException(CONFLICT);
        }
        return dialogData;
    }

    public Optional<DialogData> hentDialog(String dialogId, AktivitetId aktivitetId, AktorId aktorId) {
        if (dialogId == null && aktivitetId == null) return Optional.empty();
        if (dialogId != null && !dialogId.isEmpty()) {
            return Optional.ofNullable(hentDialog(Long.parseLong(dialogId), aktorId));
        } else {
            return Optional.ofNullable(aktivitetId)
                .filter(a -> StringUtils.isNotEmpty(a.getId()))
                .flatMap((id) -> hentDialogForAktivitetId(id, aktorId));
        }
    }

    private DialogData markerDialogSomLestAvVeileder(long dialogId) {
        var dialogData = hentDialogUtenTilgangsSjekk(dialogId);
        return dialogStatusService.markerSomLestAvVeileder(dialogData);
    }

    private DialogData markerDialogSomLestAvBruker(long dialogId) {
        var dialogData = hentDialogUtenTilgangsSjekk(dialogId);
        return dialogStatusService.markerSomLestAvBruker(dialogData);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(AktivitetId aktivitetId, AktorId aktorId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId, aktorId);
    }

    public AktorId hentAktoerIdForPerson(Person person) {
        return hentAktoerIdForPerson(person.eksternBrukerId());
    }

    public AktorId hentAktoerIdForPerson(Id person) {
        if (person instanceof Fnr) {
            return Optional
                    .ofNullable(aktorOppslagClient.hentAktorId(Fnr.of(person.get())))
                    .orElseThrow(RuntimeException::new);
        } else if (person instanceof AktorId) {
            return AktorId.of(person.get());
        }
        return null;
    }

    public Fnr hentFnrForPerson(Person person) {
        if (person instanceof Person.AktorId) {
            return Optional
                    .ofNullable(aktorOppslagClient.hentFnr(AktorId.of(person.get())))
                    .orElseThrow(RuntimeException::new);
        } else if (person instanceof Person.Fnr) {
            return Fnr.of(person.get());
        }
        return null;
    }

    public void settKontorsperredeDialogerTilHistoriske(String aktoerId, Date avsluttetDato) {
        dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(aktoerId, avsluttetDato)
                .forEach(this::oppdaterDialogTilHistorisk);

        sendPaaKafka(aktoerId);
    }

    public void settDialogerTilHistoriske(String aktoerId, UUID oppfolgingsperiodeId) {
        dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktoerId, oppfolgingsperiodeId)
                .forEach(this::oppdaterDialogTilHistorisk);

        sendPaaKafka(aktoerId);
    }

    public void sendPaaKafka(String aktorId) {
        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        var kafkaDialogMelding = KafkaDialogMelding.mapTilDialogData(dialoger, aktorId);
        kafkaProducerService.sendDialogMelding(kafkaDialogMelding);
    }

    public DialogData hentDialogUtenTilgangsSjekk(long dialogId) {
        return dialogDAO.hentDialog(dialogId);
    }

    public DialogData hentDialog(long dialogId, AktorId aktorId) {
        return dialogDAO.hentDialog(dialogId, aktorId);
    }

    private void oppdaterDialogTilHistorisk(DialogData dialogData) {
        dialogStatusService.settDialogTilHistorisk(dialogData);
    }

    public DialogData opprettDialog(NyDialog nyHenvendelseDTO) {
        UUID gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(nyHenvendelseDTO.getAktorId());
        var dialogData = DialogData.builder()
                .oppfolgingsperiode(gjeldendeOppfolgingsperiode)
                .overskrift(nyHenvendelseDTO.getOverskrift())
                .aktorId(nyHenvendelseDTO.getAktorId().get())
                .aktivitetId(AktivitetId.of(nyHenvendelseDTO.getAktivitetId()))
                .egenskaper(nyHenvendelseDTO instanceof NyEskaleringsVarselDialog
                        ? List.of(EgenskapType.ESKALERINGSVARSEL)
                        : Collections.emptyList()
                )
                .kontorsperreEnhetId(kvpService.kontorsperreEnhetId(nyHenvendelseDTO.getFnr()))
                .opprettetDato(new Date())
                .build();

        if (StringUtils.isEmpty(dialogData.getOverskrift())) {
            throw new IllegalArgumentException("Dialog må ha en overskrift");
        }
        if (StringUtils.isEmpty(dialogData.getAktorId())) {
            throw new IllegalArgumentException("Dialog må ha en aktørId");
        }
        DialogData nyDialog = dialogDAO.opprettDialog(dialogData);
        dialogStatusService.oppdaterDatavarehus(nyDialog);

        if (auth.erEksternBruker()) {
            funksjonelleMetrikker.nyDialogBruker(nyDialog);
        } else if (auth.erInternBruker()) {
            funksjonelleMetrikker.nyDialogVeileder(nyDialog);
        }

        return nyDialog;
    }

    private void slettKladd(String dialogId, String aktivitetId, Person person) {
        if (person instanceof Person.Fnr) {
            kladdService.deleteKladd(person.get(), dialogId, aktivitetId);
        }
    }

    @SneakyThrows
    public URL utledDialogLink(long id) {
        return new URL(String.format("%s/%s", dialogUrl, id));
    }
}
