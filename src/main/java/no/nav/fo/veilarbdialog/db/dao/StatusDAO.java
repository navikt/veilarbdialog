package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@Transactional
@RequiredArgsConstructor
public class StatusDAO {

    private final JdbcTemplate jdbc;
    private final DateProvider dateProvider;

    public void markerSomLestAvVeileder(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "ELDSTE_ULESTE_FOR_VEILEDER = ?, " +
                        "LEST_AV_VEILEDER_TID = "+dateProvider.getNow()+", " +
                        "OPPDATERT = "+dateProvider.getNow()+" " +
                        "where DIALOG_ID = ?",
                null,
                dialogId
        );
    }

    public void markerSomLestAvBruker(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "ULESTPARAGRAF8VARSEL = ?, " +
                        "ELDSTE_ULESTE_FOR_BRUKER = ?, " +
                        "LEST_AV_BRUKER_TID = "+dateProvider.getNow()+", " +
                        "OPPDATERT = "+dateProvider.getNow()+" " +
                        "where DIALOG_ID = ?",
                0,
                null,
                dialogId);
    }

    public void setVenterPaNavTilNaa(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_NAV_SIDEN = "+dateProvider.getNow()+", " +
                        "OPPDATERT = "+dateProvider.getNow() + " " +
                        "where DIALOG_ID = ?",
                dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNaa(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_SVAR_FRA_BRUKER = "+dateProvider.getNow()+", " +
                        "OPPDATERT = "+dateProvider.getNow()+" " +
                        "where DIALOG_ID = ?",
                dialogId);
    }

    public void setVenterPaNavTilNull(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_NAV_SIDEN = ?, " +
                        "OPPDATERT = "+dateProvider.getNow()+" " +
                        "where DIALOG_ID = ?",
                null,
                dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNull(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_SVAR_FRA_BRUKER = ?, " +
                        "OPPDATERT = "+dateProvider.getNow()+" " +
                        "where DIALOG_ID = ?",
                null,
                dialogId);
    }

    public void setEldsteUlesteForBruker(long dialogId, Date date) {
        jdbc.update("update DIALOG set " +
                        "ELDSTE_ULESTE_FOR_BRUKER = ?, " +
                        "OPPDATERT = "+dateProvider.getNow()+" " +
                        "where DIALOG_ID = ?",
                date,
                dialogId);
    }

    public void setNyMeldingFraBruker(long dialogId, Date eldsteUlesteForVeileder, Date venterPaNavSiden) {
        jdbc.update("update DIALOG set " +
                        "VENTER_PA_SVAR_FRA_BRUKER = ?, " +
                        "ELDSTE_ULESTE_FOR_VEILEDER = ?, " +
                        "VENTER_PA_NAV_SIDEN = ?, " +
                        "OPPDATERT = "+dateProvider.getNow()+" " +
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
                        "OPPDATERT = "+dateProvider.getNow()+" " +
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
