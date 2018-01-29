package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.sbl.jdbc.Database;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Transactional
public class StatusDAO {
    private static final Logger LOG = getLogger(StatusDAO.class);
    private static final String ULESTE_MELDINGER_FOR_VEILEDER_SIDEN = "uleste_meldinger_for_veileder_siden";
    private static final String VENTER_PA_NAV_SIDEN = "venter_pa_nav_siden";
    private static final String ULESTE_MELDINGER_FOR_BRUKER_SIDEN = "uleste_meldinger_for_bruker_siden";
    private static final String VENTER_PA_SVAR_FRA_BRUKER_SIDEN = "venter_pa_svar_fra_bruker_siden";
    private static final String OPPDATERT = "oppdatert";
    private static final String HISTORISK = "historisk";
    private static final String DIALOG_ID = "dialog_id";

    private final Database database;
    private final DateProvider dateProvider;

    @Inject
    StatusDAO(Database database,
                     DateProvider dateProvider) {
        this.database = database;
        this.dateProvider = dateProvider;
    }

    void oppdaterStatusForNyHenvendelse(HenvendelseData henvendelseData) {
        if (henvendelseData.avsenderType == AvsenderType.BRUKER) {
            nyMeldingFraBruker(henvendelseData.getDialogId());
        } else {
            oppdaterLestForBruker(henvendelseData.getDialogId(), false);
        }
    }

    void markerSomLestAvBruker(long dialogId) {
        oppdaterLestForBruker(dialogId, true);
    }

    void markerSomLestAvVeileder(long dialogId) {
        oppdaterLestForVeileder(dialogId, true);
    }

    void oppdaterVenterPaNav(long dialogId, boolean venter) {
        oppdaterVenterPaa(dialogId, venter, VENTER_PA_NAV_SIDEN);
    }

    public void oppdaterVenterPaSvarFraBruker(long dialogId, boolean venter) {
        oppdaterVenterPaa(dialogId, venter, VENTER_PA_SVAR_FRA_BRUKER_SIDEN);
    }

    private void nyMeldingFraBruker(long dialogId) {
        oppdaterVenterPaNav(dialogId, true);
        oppdaterLestForVeileder(dialogId, false);
    }

    private void oppdaterLestForVeileder(long dialogId, boolean erLest) {
        oppdaterLestStatus(dialogId, erLest, ULESTE_MELDINGER_FOR_VEILEDER_SIDEN);
    }

    private void oppdaterLestForBruker(long dialogId, boolean erLest) {
        oppdaterLestStatus(dialogId, erLest, ULESTE_MELDINGER_FOR_BRUKER_SIDEN);
    }

    private void oppdaterLestStatus(long dialogId, boolean erLest, String feltnavn) {
        if (erLest) {
            setFeltTilNull(feltnavn, dialogId);
        } else {
            setNowIfNull(feltnavn, dialogId);
        }
    }

    private void oppdaterVenterPaa(long dialogId, boolean venter, String feltnavn) {
        if (venter) {
            setNowIfNull(feltnavn, dialogId);
        } else {
            setFeltTilNull(feltnavn, dialogId);
        }
    }

    public void oppdaterDialogTilHistorisk(DialogData dialogData) {
        String sql = "UPDATE DIALOG SET " +
                HISTORISK + " = 1, " +
                OPPDATERT + " = " + dateProvider.getNow() + ", " +
                ULESTE_MELDINGER_FOR_BRUKER_SIDEN + " = null, " +
                ULESTE_MELDINGER_FOR_VEILEDER_SIDEN + " = null, " +
                VENTER_PA_SVAR_FRA_BRUKER_SIDEN + " = null, " +
                VENTER_PA_NAV_SIDEN + " = null";
        sql += " WHERE " + DIALOG_ID + " = ?";

        database.update(sql, dialogData.getId());
    }

    private void setNowIfNull(String feltnavn, long dialogId) {
        long antallOppdaterte = database.update("" +
                "UPDATE DIALOG SET " +
                feltnavn + " = " + dateProvider.getNow() + ", " +
                OPPDATERT + " = " + dateProvider.getNow() +
                "WHERE " + DIALOG_ID + " = ?  and " + feltnavn + " = null ", dialogId
        );
        if (antallOppdaterte == 0) {
            oppdaterSisteEndring(dialogId);
        }
    }

    private void setFeltTilNull(String feltNavn, long dialogId) {
        database.update("" +
                        "UPDATE DIALOG SET " + feltNavn + " = null, " +
                        OPPDATERT + " = " + dateProvider.getNow() +
                        "WHERE " + DIALOG_ID + " = ?  and " + feltNavn + " = null",
                dialogId
        );
    }

    private void oppdaterSisteEndring(long dialogId) {
        database.update("" +
                "UPDATE DIALOG SET " +
                OPPDATERT + " = " + dateProvider.getNow() + " " +
                "where " + DIALOG_ID + " = ?", dialogId);
    }
}
