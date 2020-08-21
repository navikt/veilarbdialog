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
                        "eldste_uleste_for_veileder = ?, " +
                        "lest_av_veileder_tid = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                null,
                dateProvider.getNow(),
                dateProvider.getNow(),
                dialogId
        );
    }

    public void markerSomLestAvBruker(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "ulestParagraf8Varsel = ?, " +
                        "eldste_uleste_for_bruker = ?, " +
                        "lest_av_bruker_tid = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                0,
                null,
                dateProvider.getNow(),
                dateProvider.getNow(),
                dialogId);
    }

    public void setVenterPaNavTilNaa(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "venter_pa_nav_siden = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                dateProvider.getNow(),
                dateProvider.getNow(),
                dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNaa(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "venter_pa_svar_fra_bruker = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                dateProvider.getNow(),
                dateProvider.getNow(),
                dialogId);
    }

    public void setVenterPaNavTilNull(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "venter_pa_nav_siden = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                null,
                dateProvider.getNow(),
                dialogId);
    }

    public void setVenterPaSvarFraBrukerTilNull(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "venter_pa_svar_fra_bruker = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                null,
                dateProvider.getNow(),
                dialogId);
    }

    public void setEldsteUlesteForBruker(long dialogId, Date date) {
        jdbc.update("update DIALOG set " +
                        "eldste_uleste_for_bruker = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                date,
                dateProvider.getNow(),
                dialogId);
    }

    public void setNyMeldingFraBruker(long dialogId, Date eldsteUlesteForVeileder, Date venterPaNavSiden) {
        jdbc.update("update DIALOG set " +
                        "venter_pa_svar_fra_bruker = ?, " +
                        "eldste_uleste_for_veileder = ?, " +
                        "venter_pa_nav_siden = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                null,
                eldsteUlesteForVeileder,
                venterPaNavSiden,
                dateProvider.getNow(),
                dialogId);
    }

    public void setHistorisk(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "venter_pa_svar_fra_bruker = ?, " +
                        "venter_pa_nav_siden = ?, " +
                        "historisk = ?, " +
                        "oppdatert = ? " +
                        "where dialog_id = ?",
                null,
                null,
                1,
                dateProvider.getNow(),
                dialogId);
    }

    public void markerSomParagraf8(long dialogId) {
        jdbc.update("update DIALOG set " +
                        "ulestParagraf8Varsel = ? " +
                        "where dialog_id = ?",
                1,
                dialogId);
    }

}
