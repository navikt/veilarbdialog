package no.nav.fo.veilarbdialog.service;

import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.StatusDAO;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
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
    private final DataVarehusDAO dataVarehusDAO;

    @Inject
    public DialogStatusService(StatusDAO statusDAO, DialogDAO dialogDAO, DataVarehusDAO dataVarehusDAO) {
        this.statusDAO = statusDAO;
        this.dialogDAO = dialogDAO;
        this.dataVarehusDAO = dataVarehusDAO;
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
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.LEST_AV_VEILEDER);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData markerSomLestAvBruker(DialogData dialogData) {
        if (dialogData.erLestAvBruker()) {
            return dialogData;
        }
        statusDAO.markerSomLestAvBruker(dialogData.getId());
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.LEST_AV_BRUKER);
        FunksjonelleMetrikker.markerDialogSomLestAvBruker(dialogData);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public DialogData oppdaterVenterPaNavSiden(DialogData dialogData, DialogStatus dialogStatus) {
        if(dialogData.erFerdigbehandlet() == dialogStatus.ferdigbehandlet){
            return dialogData;
        }

        if (dialogStatus.ferdigbehandlet) {
            statusDAO.setVenterPaNavTilNull(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData,  DatavarehusEvent.BESVART_AV_NAV);
        } else {
            statusDAO.setVenterPaNavTilNaa(dialogData.getId());
            dataVarehusDAO.insertEvent(dialogData,  DatavarehusEvent.VENTER_PAA_NAV);
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
            dataVarehusDAO.insertEvent(dialogData,  DatavarehusEvent.VENTER_PAA_BRUKER);
        } else {
            statusDAO.setVenterPaSvarFraBrukerTilNull(dialogStatus.getDialogId());
            dataVarehusDAO.insertEvent(dialogData,  DatavarehusEvent.BESVART_AV_BRUKER);
        }
        FunksjonelleMetrikker.oppdaterVenterSvar(dialogStatus);
        return dialogDAO.hentDialog(dialogStatus.getDialogId());
    }

    public DialogData settDialogTilHistorisk(DialogData dialogData) {
        statusDAO.setHistorisk(dialogData.getId());
        if(!dialogData.erFerdigbehandlet()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_NAV);
        }
        if(dialogData.venterPaSvar()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER);
        }
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.SATT_TIL_HISTORISK);
        return dialogDAO.hentDialog(dialogData.getId());
    }

    public void nyDialog(DialogData oprettet) {
        dataVarehusDAO.insertEvent(oprettet, DatavarehusEvent.DIALOG_OPPRETTET);
    }

    private void nyMeldingFraVeileder(DialogData dialogData, HenvendelseData henvendelseData) {
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.NY_HENVENDELSE_FRA_VEILEDER);

        Date eldsteUlesteForBruker = getEldsteUlesteForBruker(dialogData, henvendelseData);
        statusDAO.setEldsteUlesteForBruker(dialogData.getId(), eldsteUlesteForBruker);
    }

    private Date getEldsteUlesteForBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erLestAvBruker() ? henvendelseData.getSendt() : dialogData.getEldsteUlesteTidspunktForBruker();
    }

    private void nyMeldingFraBruker(DialogData dialogData, HenvendelseData henvendelseData) {
        dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.NY_HENVENDELSE_FRA_BRUKER);

        Date eldsteUlesteForVeileder = getEldsteUlesteForVeileder(dialogData, henvendelseData);
        Date venterPaNavSiden = dialogData.getVenterPaNavSiden();

        if(dialogData.erFerdigbehandlet()) {
            venterPaNavSiden = henvendelseData.getSendt();
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.VENTER_PAA_NAV);
        }
        if(dialogData.venterPaSvar()) {
            dataVarehusDAO.insertEvent(dialogData, DatavarehusEvent.BESVART_AV_BRUKER);
        }

        statusDAO.setNyMeldingFraBruker(
                dialogData.getId(),
                eldsteUlesteForVeileder,
                venterPaNavSiden
        );
    }

    private Date getEldsteUlesteForVeileder(DialogData dialogData, HenvendelseData henvendelseData) {
        return dialogData.erLestAvVeileder() ? henvendelseData.getSendt() : dialogData.getEldsteUlesteTidspunktForVeileder();
    }
}
