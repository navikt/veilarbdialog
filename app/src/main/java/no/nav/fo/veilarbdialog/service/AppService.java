package no.nav.fo.veilarbdialog.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class AppService {

    private final AktorService aktorService;
    private final DialogDAO dialogDAO;
    private final StatusService statusService;
    private final DialogFeedDAO dialogFeedDAO;
    private final PepClient pepClient;
    private final KvpClient kvpClient;

    public AppService(AktorService aktorService,
                      DialogDAO dialogDAO,
                      StatusService statusService,
                      DialogFeedDAO dialogFeedDAO,
                      PepClient pepClient, KvpClient kvpClient) {
        this.aktorService = aktorService;
        this.dialogDAO = dialogDAO;
        this.statusService = statusService;
        this.dialogFeedDAO = dialogFeedDAO;
        this.pepClient = pepClient;
        this.kvpClient = kvpClient;
    }

    /**
     * Returnerer en kopi av AktivitetData-objektet hvor kontorsperreEnhetId
     * er satt dersom brukeren er under KVP.
     */
    private DialogData tagMedKontorsperre(DialogData dialogData) {
        return Optional.ofNullable(kvpClient.kontorsperreEnhetId(dialogData.getAktorId()))
                .map(dialogData::withKontorsperreEnhetId)
                .orElse(dialogData);
    }

    private HenvendelseData tagMedKontorsperre(HenvendelseData henvendelseData, DialogData dialogData) {
        return Optional.ofNullable(kvpClient.kontorsperreEnhetId(dialogData.getAktorId()))
                .map(henvendelseData::withKontorsperreEnhetId)
                .orElse(henvendelseData);
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(String ident) {
        sjekkTilgangTilFnr(ident);
        return dialogDAO.hentDialogerForAktorId(hentAktoerIdForIdent(ident));
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData) {
        sjekkTilgangTilAktorId(dialogData.getAktorId());
        DialogData kontorsperretDialog = tagMedKontorsperre(dialogData);
        long dialogId = dialogDAO.opprettDialog(kontorsperretDialog);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        long dialogId = henvendelseData.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        HenvendelseData kontorsperretHenvendelse = tagMedKontorsperre(henvendelseData, dialogData);
        HenvendelseData opprettetHenvendelse = dialogDAO.opprettHenvendelse(kontorsperretHenvendelse);
        statusService.nyHenvendelse(dialogData, opprettetHenvendelse);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    @Transactional(readOnly = true)
    public DialogData hentDialog(long dialogId) {
        DialogData dialogData = hentDialogUtenTilgangskontroll(dialogId);
        sjekkLeseTilgangTilDialog(dialogData);
        return dialogData;
    }

    private DialogData hentDialogUtenTilgangskontroll(long dialogId) {
        return dialogDAO.hentDialog(dialogId);
    }

    public DialogData markerDialogSomLestAvVeileder(long dialogId) {
        DialogData dialogData = sjekkLeseTilgangTilDialog(dialogId);
        dialogDAO.markerDialogSomLestAvVeileder(dialogId);
        statusService.markerSomLestAvVeileder(dialogData);
        FunksjonelleMetrikker.markerSomLestAvVeilederMetrikk(dialogData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData markerDialogSomLestAvBruker(long dialogId) {
        DialogData dialogData = sjekkLeseTilgangTilDialog(dialogId);
        dialogDAO.markerDialogSomLestAvBruker(dialogId);
        statusService.markerSomLestAvBruker(dialogData);
        FunksjonelleMetrikker.merkDialogSomLestAvBrukerMetrikk(dialogData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        statusService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);
        FunksjonelleMetrikker.oppdaterFerdigbehandletTidspunktMetrikk(dialogData, dialogStatus);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatus);
        statusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);
        FunksjonelleMetrikker.oppdaterVenterSvarMetrikk(dialogStatus, dialogData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId).map(this::sjekkLeseTilgangTilDialog);
    }

    public String hentAktoerIdForIdent(String ident) {
        // NB: ingen tilgangskontroll på dette oppslaget
        return aktorService.getAktorId(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    @Transactional(readOnly = true)
    public List<DialogAktor> hentAktorerMedEndringerFOM(Date tidspunkt, int pageSize) {
        // NB: ingen tilgangskontroll her siden feed har egen mekanisme for dette
        return dialogFeedDAO.hentAktorerMedEndringerFOM(tidspunkt, pageSize);
    }

    public void settDialogerTilHistoriske(String aktoerId, Date avsluttetDato) {
        // NB: ingen tilgangskontroll, brukes av vår feed-consumer
        dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktoerId, avsluttetDato)
                .forEach(this::oppdaterDialogTilHistorisk);

        updateDialogAktorFor(aktoerId);
    }

    public void updateDialogAktorFor(String aktorId) {
        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        dialogFeedDAO.updateDialogAktorFor(aktorId, dialoger);
    }

    private void oppdaterDialogTilHistorisk(DialogData dialogData) {
        dialogDAO.oppdaterDialogTilHistorisk(dialogData);
        statusService.settDialogTilHistorisk(dialogData);
    }

    private String sjekkTilgangTilFnr(String ident) {
        return pepClient.sjekkLeseTilgangTilFnr(ident);
    }

    private void sjekkTilgangTilAktorId(String aktorId) {
        sjekkTilgangTilFnr(aktorService.getFnr(aktorId).orElseThrow(IngenTilgang::new));
    }

    private DialogData sjekkLeseTilgangTilDialog(DialogData dialogData) {
        sjekkTilgangTilAktorId(dialogData.getAktorId());
        return dialogData;
    }

    private DialogData sjekkLeseTilgangTilDialog(long id) {
        return hentDialog(id);
    }

    private DialogData sjekkSkriveTilgangTilDialog(long id) {
        DialogData dialogData = hentDialog(id);
        if (dialogData.isHistorisk()) {
            throw new UlovligHandling();
        }
        return dialogData;
    }


}
