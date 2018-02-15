package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.fo.veilarbdialog.domain.BooleanUpdateEnum.TRUE;
import static no.nav.fo.veilarbdialog.domain.DateUpdateEnum.NOW;
import static no.nav.fo.veilarbdialog.domain.DateUpdateEnum.NULL;
import static no.nav.fo.veilarbdialog.domain.DateUpdateEnum.NYESTE_HENVENDELSE;

@Component
public class MetadataService {
    private final DialogDAO dialogDAO;

    @Inject
    public MetadataService(DialogDAO dialogDAO) {
        this.dialogDAO = dialogDAO;
    }


    public DialogData nyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
        if (henvendelseData.getSendt() == null) {
            throw new IllegalArgumentException("HenvendelseData må ha Sendt-tidspunkt");
        }

        DialogStatusOppdaterer status = lagStatusForNyHenvendelse(dialogData, henvendelseData);
        return dialogDAO.oppdaterStatus(status);
    }

    public DialogData markerSomLestAvVeileder(DialogData dialogData) {
        if (dialogData.erLestAvVeileder()) {
            return dialogData;
        }
        DialogStatusOppdaterer oppdaterer = new DialogStatusOppdaterer(dialogData.getId());
        oppdaterer.lestAvVeileder();
        FunksjonelleMetrikker.markerDialogSomLestAvVeileder(dialogData);

        return dialogDAO.oppdaterStatus(oppdaterer);
    }

    public DialogData markerSomLestAvBruker(DialogData dialogData) {
        if (dialogData.erLestAvBruker()) {
            return dialogData;
        }
        DialogStatusOppdaterer oppdaterer = new DialogStatusOppdaterer(dialogData.getId());
        oppdaterer.lestAvBruker();
        FunksjonelleMetrikker.markerDialogSomLestAvBruker(dialogData);
        return dialogDAO.oppdaterStatus(oppdaterer);
    }

    public DialogData oppdaterVenterPaNavSiden(DialogData dialogData, DialogStatus dialogStatus) {
        DialogStatusOppdaterer oppdaterer = new DialogStatusOppdaterer(dialogStatus.getDialogId());
        if (dialogStatus.ferdigbehandlet) {
            oppdaterer.setVenterPaNavSiden(NULL);
        } else {
            oppdaterer.setVenterPaNavSiden(NOW);
        }
        FunksjonelleMetrikker.oppdaterFerdigbehandletTidspunkt(dialogData, dialogStatus);
        return dialogDAO.oppdaterStatus(oppdaterer);
    }

    public DialogData oppdaterVenterPaSvarFraBrukerSiden(DialogStatus dialogStatus) {
        DialogStatusOppdaterer oppdaterer = new DialogStatusOppdaterer(dialogStatus.getDialogId());
        if (dialogStatus.venterPaSvar) {
            oppdaterer.setVenterPaSvarFraBruker(NOW);
        } else {
            oppdaterer.setVenterPaSvarFraBruker(NULL);
        }
        FunksjonelleMetrikker.oppdaterVenterSvar(dialogStatus);
        return dialogDAO.oppdaterStatus(oppdaterer);
    }

    public DialogData settDialogTilHistorisk(DialogData dialogData) {
        DialogStatusOppdaterer oppdaterer = new DialogStatusOppdaterer(dialogData.getId());
        oppdaterer.setHistorisk(TRUE);
        oppdaterer.setVenterPaNavSiden(NULL);
        oppdaterer.setVenterPaSvarFraBruker(NULL);
        return dialogDAO.oppdaterStatus(oppdaterer);
    }

    private DialogStatusOppdaterer lagStatusForNyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
        DialogStatusOppdaterer oppdaterer;
        if (henvendelseData.fraBruker()) {
            oppdaterer = nyHenvendelseFraBruker(dialogData);
            FunksjonelleMetrikker.nyHenvendelseBruker(dialogData);
        } else {
            oppdaterer = nyHenvendelseFraVeileder(dialogData);
            FunksjonelleMetrikker.nyHenvendelseVeileder(dialogData);
        }
        return oppdaterer;
    }

    private DialogStatusOppdaterer nyHenvendelseFraVeileder(DialogData dialogData) {
        DialogStatusOppdaterer oppdaterer = new DialogStatusOppdaterer(dialogData.getId());
        if (dialogData.erLestAvBruker()) {
            oppdaterer.setEldsteUlesteForBruker(NYESTE_HENVENDELSE);
        }
        return oppdaterer;
    }

    private DialogStatusOppdaterer nyHenvendelseFraBruker(DialogData dialogData) {
        DialogStatusOppdaterer oppdaterer = new DialogStatusOppdaterer(dialogData.getId());
        if (dialogData.venterPaSvar()) {
            oppdaterer.setVenterPaSvarFraBruker(NULL);
        }
        if (dialogData.erLestAvVeileder()) {
            oppdaterer.setEldsteUlesteForVeileder(NYESTE_HENVENDELSE);
        }
        if (dialogData.erFerdigbehandlet()) {
            oppdaterer.setVenterPaNavSiden(NYESTE_HENVENDELSE);
        }
        return oppdaterer;
    }
}
