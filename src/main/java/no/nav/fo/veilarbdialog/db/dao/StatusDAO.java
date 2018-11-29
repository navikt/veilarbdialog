package no.nav.fo.veilarbdialog.db.dao;

import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Date;

import static no.nav.fo.veilarbdialog.db.dao.DBKonstanter.*;

@Component
@Transactional
public class StatusDAO {

    private final Database db;
    private final DateProvider dateProvider;

    @Inject
    public StatusDAO(Database db, DateProvider dateProvider) {
        this.db = db;
        this.dateProvider = dateProvider;
    }

    public void markerSomLestAvVeileder(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                ELDSTE_ULESTE_FOR_VEILEDER + " = null, " +
                LEST_AV_VEILEDER_TID + " = " + dateProvider.getNow() + ", " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }

    public void markerSomLestAvBruker(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                HAR_ULEST_PARAGRAF_8 + " = 0, " +
                ELDSTE_ULESTE_FOR_BRUKER + " = null, " +
                LEST_AV_BRUKER_TID + " = " + dateProvider.getNow() + ", " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }

    public void setVenterPaNavTilNaa(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                VENTER_PA_NAV_SIDEN + " = " + dateProvider.getNow() + ", " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNaa(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                VENTER_PA_SVAR_FRA_BRUKER + " = " + dateProvider.getNow() + ", " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }

    public void setVenterPaNavTilNull(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                VENTER_PA_NAV_SIDEN + " = null, " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNull(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                VENTER_PA_SVAR_FRA_BRUKER + " = null, " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }

    public void setEldsteUlesteForBruker(long dialogId, Date date) {
        db.update("UPDATE DIALOG SET " +
                ELDSTE_ULESTE_FOR_BRUKER + " = ?, " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", date, dialogId);
    }

    public void setNyMeldingFraBruker(long dialogId, Date eldsteUlesteForVeileder, Date venterPaNavSiden) {
        db.update("UPDATE DIALOG SET " +
                VENTER_PA_SVAR_FRA_BRUKER + " = null, " +
                ELDSTE_ULESTE_FOR_VEILEDER + " = ?, " +
                VENTER_PA_NAV_SIDEN + " = ?, " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", eldsteUlesteForVeileder, venterPaNavSiden, dialogId);
    }

    public void setHistorisk(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                VENTER_PA_SVAR_FRA_BRUKER + " = null, " +
                VENTER_PA_NAV_SIDEN + " = null, " +
                HISTORISK + " = 1, " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }

    public void markerSomParagraf8(long dialogId) {
        db.update("UPDATE DIALOG SET " +
                HAR_ULEST_PARAGRAF_8 + " = 1 " +
                "WHERE " + DIALOG_ID + " = ?", dialogId);
    }
}
