package no.nav.fo.veilarbdialog.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.ws.consumer.AktoerConsumer;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class AppService {

    @Inject
    private AktoerConsumer aktoerConsumer;

    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private PepClient pepClient;

    public List<DialogData> hentDialogerForBruker(String ident) {
        sjekkTilgangTilFnr(ident);
        return dialogDAO.hentDialogerForAktorId(hentAktoerIdForIdent(ident));
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData) {
        sjekkTilgangTilAktorId(dialogData.getAktorId());
        return hentDialog(dialogDAO.opprettDialog(dialogData));
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        sjekkTilgangTilDialog(henvendelseData.dialogId);
        dialogDAO.opprettHenvendelse(henvendelseData);
        return hentDialog(henvendelseData.dialogId);
    }

    public DialogData hentDialog(long dialogId) {
        DialogData dialogData = dialogDAO.hentDialog(dialogId);
        sjekkTilgangTilDialog(dialogData);
        return dialogData;
    }

    public DialogData markerDialogSomLestAvVeileder(long dialogId) {
        sjekkTilgangTilDialog(dialogId);
        dialogDAO.markerDialogSomLestAvVeileder(dialogId);
        return hentDialog(dialogId);
    }

    public DialogData markerDialogSomLestAvBruker(long dialogId) {
        sjekkTilgangTilDialog(dialogId);
        dialogDAO.markerDialogSomLestAvBruker(dialogId);
        return hentDialog(dialogId);
    }

    public DialogData oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        sjekkTilgangTilDialog(dialogStatus.dialogId);
        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        return hentDialog(dialogStatus.dialogId);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        sjekkTilgangTilDialog(dialogStatus.dialogId);
        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatus);
        return hentDialog(dialogStatus.dialogId);
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId).map(this::sjekkTilgangTilDialog);
    }

    public String hentAktoerIdForIdent(String ident) {
        sjekkTilgangTilFnr(ident);
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    public List<DialogAktor> hentAktorerMedEndringerFOM(Date tidspunkt) {
        // NB: ingen tilgangskontroll her siden feed har egen mekanisme for dette
        return dialogDAO.hentAktorerMedEndringerFOM(tidspunkt);
    }

    public void settDialogerTilHistoriske(AvsluttetOppfolgingFeedDTO element) {
        // NB: ingen tilgangskontroll, brukes av vår feed-consumer
        dialogDAO.hentGjeldendeDialogerForAktorId(element.getAktoerid())
                .forEach(dialog -> dialogDAO.settDialogTilHistoriskOgOppdaterFeed(dialog));
    }

    protected String sjekkTilgangTilFnr(String ident) {
        return pepClient.sjekkTilgangTilFnr(ident);
    }

    protected void sjekkTilgangTilAktorId(String aktorId) {
        sjekkTilgangTilFnr(aktoerConsumer.hentIdentForAktorId(aktorId).orElseThrow(IngenTilgang::new));
    }

    protected DialogData sjekkTilgangTilDialog(DialogData dialogData) {
        sjekkTilgangTilAktorId(dialogData.getAktorId());
        return dialogData;
    }

    protected void sjekkTilgangTilDialog(long id) {
        hentDialog(id);
    }

}



