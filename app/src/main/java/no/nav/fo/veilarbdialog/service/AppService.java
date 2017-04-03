package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.ws.consumer.AktoerConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class AppService {

    @Inject
    private AktoerConsumer aktoerConsumer;

    @Inject
    private DialogDAO dialogDAO;

    public List<DialogData> hentDialogerForBruker(String ident) {
        return dialogDAO.hentDialogerForAktorId(hentAktoerIdForIdent(ident));
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData, String ident) {
        return dialogDAO.opprettDialog(dialogData.toBuilder()
                .aktorId(hentAktoerIdForIdent(ident))
                .build()
        );
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        dialogDAO.opprettHenvendelse(henvendelseData);
        return dialogDAO.hentDialog(henvendelseData.dialogId);
    }

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

}



