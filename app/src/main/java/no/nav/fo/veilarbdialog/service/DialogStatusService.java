package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.StatusDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;

@Component
public class DialogStatusService {
    private final StatusDAO statusDAO;
    private final DialogDAO dialogDAO;

    @Inject
    public DialogStatusService(StatusDAO statusDAO, DialogDAO dialogDAO) {
        this.statusDAO = statusDAO;
        this.dialogDAO = dialogDAO;
    }

    public DialogData nyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
        if(henvendelseData.getSendt() == null){
            throw new UnsupportedOperationException("sendt tidspunkt kan ikke være null");
        }
        if (henvendelseData.fraBruker()) {
            nyMeldingFraBruker(dialogData, henvendelseData);
        } else {
            nyMeldingFraVeileder(dialogData, henvendelseData);
        }
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData markerSomLestAvVeileder(DialogData dialogData) {
        if (dialogData.erLestAvVeileder()) {
            return dialogData;
        }
        FunksjonelleMetrikker.markerDialogSomLestAvVeileder(dialogData);
        statusDAO.markerSomLestAvVeileder(dialogData.getId());
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData markerSomLestAvBruker(DialogData dialogData) {
        if (dialogData.erLestAvBruker()) {
            return dialogData;
        }
        statusDAO.markerSomLestAvBruker(dialogData.getId());
        FunksjonelleMetrikker.markerDialogSomLestAvBruker(dialogData);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData oppdaterVenterPaNavSiden(DialogData dialogData, DialogStatus dialogStatus) {
        if(dialogData.erFerdigbehandlet() == dialogStatus.ferdigbehandlet) {
            return dialogData;
        }
        if (dialogStatus.ferdigbehandlet) {
            statusDAO.setVenterPaNavTilNull(dialogData.getId());
        } else {
            statusDAO.setVenterPaNavTilNaa(dialogData.getId());
        }
        FunksjonelleMetrikker.oppdaterFerdigbehandletTidspunkt(dialogData, dialogStatus);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData oppdaterVenterPaSvarFraBrukerSiden(DialogData dialogData, DialogStatus dialogStatus) {
        if(dialogData.venterPaSvar() == dialogStatus.venterPaSvar) {
            return dialogData;
        }
        if (dialogStatus.venterPaSvar) {
            statusDAO.setVenterPaSvarFraBrukerTilNaa(dialogStatus.getDialogId());
        } else {
            statusDAO.setVenterPaSvarFraBrukerTilNull(dialogStatus.getDialogId());
        }
        FunksjonelleMetrikker.oppdaterVenterSvar(dialogStatus);
        return dialogDAO.hentDialog(dialogStatus.getDialogId());
    }

    public DialogData settDialogTilHistorisk(DialogData dialogData) {
        statusDAO.setHistorisk(dialogData.getId());
        return dialogDAO.hentDialog(dialogData.getId());
    }

    private void nyMeldingFraVeileder(DialogData dialogData, HenvendelseData henvendelseData) {
        Date eldsteUlesteForBruker = getEldsteUlesteForBruker(dialogData, henvendelseData);
        statusDAO.setEldsteUlesteForBruker(dialogData.getId(), eldsteUlesteForBruker);
        FunksjonelleMetrikker.nyHenvendelseVeileder(dialogData);
    }

    private Date getEldsteUlesteForBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erLestAvBruker() ? henvendelseData.getSendt() : dialogData.getEldsteUlesteTidspunktForBruker();
    }

    private void nyMeldingFraBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        Date eldsteUlesteForVeileder = getEldsteUlesteForVeileder(dialogData, henvendelseData);
        Date venterPaNavSiden = getVenterPaNavSiden(dialogData, henvendelseData);
        statusDAO.setNyMeldingFraBruker(
                dialogData.getId(),
                eldsteUlesteForVeileder,
                venterPaNavSiden
        );
        FunksjonelleMetrikker.nyHenvendelseBruker(dialogData);
    }

    private Date getVenterPaNavSiden(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erFerdigbehandlet() ? henvendelseData.getSendt() : dialogData.getVenterPaNavSiden();
    }

    private Date getEldsteUlesteForVeileder(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erLestAvVeileder() ? henvendelseData.getSendt() : dialogData.getEldsteUlesteTidspunktForVeileder();
    }
}
