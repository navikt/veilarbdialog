package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.*;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StatusDAO {
    private DialogDAO dialogDAO;

    @Inject
    StatusDAO(DialogDAO dialogDAO) {
        this.dialogDAO = dialogDAO;
    }

    public void nyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
        if (henvendelseData.getSendt() == null) {
            throw new IllegalArgumentException("HenvendelseData må ha Sendt-tidspunkt");
        }

        Status status = oppdaterStatusForNyHenvendelse(dialogData, henvendelseData);
        dialogDAO.oppdaterStatus(status);
    }

    public void markerSomLestAvVeileder(DialogData dialogData) {
        Status status = getStatus(dialogData);
        if (status.eldsteUlesteForVeileder == null) {
            return;
        }

        status.eldsteUlesteForVeileder = null;
        dialogDAO.oppdaterStatus(status);
    }

    public void markerSomLestAvBruker(DialogData dialogData) {
        Status status = getStatus(dialogData);
        if (status.eldsteUlesteForBruker == null) {
            return;
        }

        status.eldsteUlesteForBruker = null;
        dialogDAO.oppdaterStatus(status);
    }

    public void oppdaterVenterPaNavSiden(DialogData dialogData, DialogStatus dialogStatus) {
        Status status = getStatus(dialogData);
        if (dialogStatus.ferdigbehandlet) {
            status.venterPaNavSiden = null;
        } else {

            status.setVenterPaNavSiden();
        }
        dialogDAO.oppdaterStatus(status);
    }

    public void oppdaterVenterPaSvarFraBrukerSiden(DialogData dialogData, DialogStatus dialogStatus) {
        Status status = getStatus(dialogData);
        if (dialogStatus.venterPaSvar) {
            status.setVenterPaSvarFraBruker();
        } else {
            status.venterPaSvarFraBruker = null;
        }
        dialogDAO.oppdaterStatus(status);
    }

    public void settDialogTilHistorisk(DialogData dialogData) {
        Status status = new Status(dialogData.getId());
        status.setHistorisk(true);
        dialogDAO.oppdaterStatus(status);
    }

    private Status oppdaterStatusForNyHenvendelse(DialogData dialogData, HenvendelseData henvendelseData) {
        Status status = getStatus(dialogData);
        if (henvendelseData.getAvsenderType() == AvsenderType.BRUKER) {
            status.setVenterPaNavSiden();
            status.setUlesteMeldingerForVeileder(henvendelseData.getSendt());
        } else {
            status.setUlesteMeldingerForBruker(henvendelseData.getSendt());
        }
        return status;
    }

    static Status getStatus(DialogData dialogData) {
        Status status = new Status(dialogData.getId());
        status.venterPaNavSiden = dialogData.getVenterPaNavSiden();
        status.venterPaSvarFraBruker = dialogData.getVenterPaSvarFraBrukerSiden();
        status.eldsteUlesteForBruker = dialogData.getEldsteUlesteTidspunktForBruker();
        status.eldsteUlesteForVeileder = dialogData.getEldsteUlesteTidspunktForVeileder();
        status.setHistorisk(dialogData.isHistorisk());
        return status;
    }
}
