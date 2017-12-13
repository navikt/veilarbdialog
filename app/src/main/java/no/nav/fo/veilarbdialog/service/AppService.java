package no.nav.fo.veilarbdialog.service;

import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.FunkjsonelleMetrikker;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class AppService {

    private final AktorService aktorService;
    private final DialogDAO dialogDAO;
    private final DialogFeedDAO dialogFeedDAO;
    private final PepClient pepClient;

    public AppService(AktorService aktorService,
                      DialogDAO dialogDAO,
                      DialogFeedDAO dialogFeedDAO,
                      PepClient pepClient) {
        this.aktorService = aktorService;
        this.dialogDAO = dialogDAO;
        this.dialogFeedDAO = dialogFeedDAO;
        this.pepClient = pepClient;
    }

    public List<DialogData> hentDialogerForBruker(String ident) {
        sjekkTilgangTilFnr(ident);
        return dialogDAO.hentDialogerForAktorId(hentAktoerIdForIdent(ident));
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData) {
        sjekkTilgangTilAktorId(dialogData.getAktorId());
        long dialogId = dialogDAO.opprettDialog(dialogData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        long dialogId = henvendelseData.dialogId;
        sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.opprettHenvendelse(henvendelseData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

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
        FunkjsonelleMetrikker.markerSomLestAvVeilederMetrikk(dialogData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData markerDialogSomLestAvBruker(long dialogId) {
        DialogData dialogData = sjekkLeseTilgangTilDialog(dialogId);
        dialogDAO.markerDialogSomLestAvBruker(dialogId);
        FunkjsonelleMetrikker.merkDialogSomLestAvBrukerMetrikk(dialogData);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        DialogData dialog = sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        FunkjsonelleMetrikker.oppdaterFerdigbehandletTidspunktMetrikk(dialog, dialogStatus);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        sjekkSkriveTilgangTilDialog(dialogId);
        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatus);
        return hentDialogUtenTilgangskontroll(dialogId);
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId).map(this::sjekkLeseTilgangTilDialog);
    }

    public String hentAktoerIdForIdent(String ident) {
        // NB: ingen tilgangskontroll på dette oppslaget
        return aktorService.getAktorId(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    public List<DialogAktor> hentAktorerMedEndringerFOM(Date tidspunkt, int pageSize) {
        // NB: ingen tilgangskontroll her siden feed har egen mekanisme for dette
        return dialogFeedDAO.hentAktorerMedEndringerFOM(tidspunkt, pageSize);
    }

    public void settDialogerTilHistoriske(String aktoerId, Date avsluttetDato) {
        // NB: ingen tilgangskontroll, brukes av vår feed-consumer
        dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktoerId, avsluttetDato)
                .forEach(dialogDAO::oppdaterDialogTilHistorisk);

        updateDialogAktorFor(aktoerId);
    }

    public void updateDialogAktorFor(String aktorId) {
        val dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        dialogFeedDAO.updateDialogAktorFor(aktorId, dialoger);
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
