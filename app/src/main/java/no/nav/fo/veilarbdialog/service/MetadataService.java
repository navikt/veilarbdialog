package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.UtilDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.domain.Status;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class MetadataService {
    private final DialogDAO dialogDAO;
    private final UtilDAO utilDAO;

    @Inject
    public MetadataService(DialogDAO dialogDAO, UtilDAO utilDAO) {
        this.dialogDAO = dialogDAO;
        this.utilDAO = utilDAO;
    }


    public DialogData nyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
        if (henvendelseData.getSendt() == null) {
            throw new IllegalArgumentException("HenvendelseData må ha Sendt-tidspunkt");
        }

        Status status = lagStatusForNyHenvendelse(dialogData, henvendelseData);
        return dialogDAO.oppdaterStatus(status);
    }

    public DialogData markerSomLestAvVeileder(DialogData dialogData) {
        Status status = getStatus(dialogData);
        if (status.eldsteUlesteForVeileder == null) {
            return dialogData;
        }
        FunksjonelleMetrikker.markerDialogSomLestAvVeileder(dialogData);

        status.eldsteUlesteForVeileder = null;
        status.setLestAvVeilederTid(utilDAO.getTimestampFromDB());

        return dialogDAO.oppdaterStatus(status);
    }

    public DialogData markerSomLestAvBruker(DialogData dialogData) {
        Status status = getStatus(dialogData);
        if (status.eldsteUlesteForBruker == null) {
            return dialogData;
        }
        FunksjonelleMetrikker.markerDialogSomLestAvBruker(dialogData);
        status.eldsteUlesteForBruker = null;
        status.setLestAvBrukerTid(utilDAO.getTimestampFromDB());
        return dialogDAO.oppdaterStatus(status);
    }

    public DialogData oppdaterVenterPaNavSiden(DialogData dialogData, DialogStatus dialogStatus) {
        Status status = getStatus(dialogData);
        if (dialogStatus.ferdigbehandlet) {
            status.venterPaNavSiden = null;
        } else {
            status.settVenterPaNavSiden(utilDAO.getTimestampFromDB());
        }

        FunksjonelleMetrikker.oppdaterFerdigbehandletTidspunkt(dialogData, dialogStatus);
        return dialogDAO.oppdaterStatus(status);
    }

    public DialogData oppdaterVenterPaSvarFraBrukerSiden(DialogData dialogData, DialogStatus dialogStatus) {
        Status status = getStatus(dialogData);
        if (dialogStatus.venterPaSvar) {
            status.settVenterPaSvarFraBruker(utilDAO.getTimestampFromDB());
        } else {
            status.venterPaSvarFraBruker = null;
        }
        FunksjonelleMetrikker.oppdaterVenterSvar(dialogStatus);
        return dialogDAO.oppdaterStatus(status);
    }

    public DialogData settDialogTilHistorisk(DialogData dialogData) {
        Status status = new Status(dialogData.getId());
        status.setHistorisk(true);
        return dialogDAO.oppdaterStatus(status);
    }

    public static Status getStatus(DialogData dialogData) {
        Status status = new Status(dialogData.getId());
        status.venterPaNavSiden = dialogData.getVenterPaNavSiden();
        status.venterPaSvarFraBruker = dialogData.getVenterPaSvarFraBrukerSiden();
        status.eldsteUlesteForBruker = dialogData.getEldsteUlesteTidspunktForBruker();
        status.eldsteUlesteForVeileder = dialogData.getEldsteUlesteTidspunktForVeileder();
        status.setHistorisk(dialogData.isHistorisk());
        return status;
    }

    private Status lagStatusForNyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
        Status status = getStatus(dialogData);
        if (henvendelseData.fraBruker()) {
            status.settVenterPaNavSiden(henvendelseData.getSendt());
            status.resetVenterPaBrukerSiden();
            status.setLestAvBrukerTid(henvendelseData.getSendt());
            FunksjonelleMetrikker.nyHenvendelseBruker(dialogData);
        } else {
            status.setUlesteMeldingerForBruker(henvendelseData.getSendt());
            status.setLestAvVeilederTid(henvendelseData.getSendt());
            FunksjonelleMetrikker.nyHenvendelseVeileder(dialogData);
        }
        return status;
    }
}
