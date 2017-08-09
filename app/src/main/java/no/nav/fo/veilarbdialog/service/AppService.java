package no.nav.fo.veilarbdialog.service;

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

    public List<DialogData> hentDialogerForBruker(String ident) {
        return dialogDAO.hentDialogerForAktorId(hentAktoerIdForIdent(ident));
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData) {
        return hentDialog(dialogDAO.opprettDialog(dialogData));
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        dialogDAO.opprettHenvendelse(henvendelseData);
        return hentDialog(henvendelseData.dialogId);
    }

    public DialogData hentDialog(long dialogId) {
        return dialogDAO.hentDialog(dialogId);
    }

    public DialogData markerDialogSomLestAvVeileder(long dialogId) {
        dialogDAO.markerDialogSomLestAvVeileder(dialogId);
        return hentDialog(dialogId);
    }

    public DialogData markerDialogSomLestAvBruker(long dialogId) {
        dialogDAO.markerDialogSomLestAvBruker(dialogId);
        return hentDialog(dialogId);
    }

    public DialogData oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        return hentDialog(dialogStatus.dialogId);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatus);
        return hentDialog(dialogStatus.dialogId);
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId);
    }

    public String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    public List<DialogAktor> hentAktorerMedEndringerFOM(Date tidspunkt) {
        return dialogDAO.hentAktorerMedEndringerFOM(tidspunkt);
    }

    public void settDialogerTilHistoriske(AvsluttetOppfolgingFeedDTO element) {
        dialogDAO.hentGjeldendeDialogerForAktorId(element.getAktoerid())
                .forEach(dialog -> dialogDAO.settDialogTilHistoriskOgOppdaterFeed(dialog));
    }
}



