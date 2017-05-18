package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.ws.consumer.AktoerConsumer;
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

    public DialogData oppdaterDialogStatus(DialogStatus dialogStatus) {
        dialogDAO.oppdaterDialogStatus(dialogStatus);
        return hentDialog(dialogStatus.dialogId);
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId);
    }

    public String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    public List<DialogAktor> hentAktorerMedEndringerEtter(Date tidspunkt) {
        return dialogDAO.hentAktorerMedEndringerEtter(tidspunkt);
    }

}



