package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@Transactional
@RequiredArgsConstructor
@java.lang.SuppressWarnings("squid:S1192")
public class StatusDAO {

    private final JdbcTemplate jdbc;

    public void markerSomLestAvVeileder(long dialogId, Date lestTidspunkt) {
        jdbc.update("update DIALOG set " +
                        "ELDSTE_ULESTE_FOR_VEILEDER = ?, " +
                        "LEST_AV_VEILEDER_TID = ? , " +
                        "OPPDATERT = ? " +
                        "where DIALOG_ID = ?",
                null,
                lestTidspunkt,
                lestTidspunkt,
                dialogId
        );
    }

    public void markerSomLestAvBruker(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "ULESTPARAGRAF8VARSEL = ?, " +
                        "ELDSTE_ULESTE_FOR_BRUKER = ?, " +
                        "LEST_AV_BRUKER_TID = CURRENT_TIMESTAMP , " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                0,
                null,
                dialogId);
    }

    public void setVenterPaNavTilNaa(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_NAV_SIDEN = CURRENT_TIMESTAMP , " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNaa(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_SVAR_FRA_BRUKER = CURRENT_TIMESTAMP , " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                dialogId);
    }

    public void setVenterPaNavTilNull(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_NAV_SIDEN = ?, " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                null,
                dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNull(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_SVAR_FRA_BRUKER = ?, " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                null,
                dialogId);
    }

    public void setEldsteUlesteForBruker(long dialogId, Date date) {
        jdbc.update("update DIALOG set " +
                        "ELDSTE_ULESTE_FOR_BRUKER = ?, " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                date,
                dialogId);
    }

    public void setNyMeldingFraBruker(long dialogId, Date eldsteUlesteForVeileder, Date venterPaNavSiden) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_SVAR_FRA_BRUKER = ?, " +
                        "ELDSTE_ULESTE_FOR_VEILEDER = ?, " +
                        "VENTER_PA_NAV_SIDEN = ?, " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                null,
                eldsteUlesteForVeileder,
                venterPaNavSiden,
                dialogId);
    }

    public void setHistorisk(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_SVAR_FRA_BRUKER = ?, " +
                        "VENTER_PA_NAV_SIDEN = ?, " +
                        "HISTORISK = ?, " +
                        "OPPDATERT = CURRENT_TIMESTAMP " +
                        "where DIALOG_ID = ?",
                null,
                null,
                1,
                dialogId);
    }

    public void markerSomParagraf8(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "ULESTPARAGRAF8VARSEL = ? " +
                        "where DIALOG_ID = ?",
                1,
                dialogId);
    }

}
