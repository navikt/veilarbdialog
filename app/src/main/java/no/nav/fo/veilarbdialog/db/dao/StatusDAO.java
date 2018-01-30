package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.sbl.jdbc.Database;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

import static no.nav.fo.veilarbdialog.db.dao.DBKonstanter.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Transactional
public class StatusDAO {
    private static final Logger LOG = getLogger(StatusDAO.class);

    private final Database database;
    private final DateProvider dateProvider;

    @Inject
    StatusDAO(Database database,
              DateProvider dateProvider) {
        this.database = database;
        this.dateProvider = dateProvider;
    }

    void oppdaterStatusForNyHenvendelse(HenvendelseData henvendelseMedId) {
        if (henvendelseMedId.avsenderType == AvsenderType.BRUKER) {
            nyHenvendelseFraBruker(henvendelseMedId);
        } else {
            nyHenvendelseFraVeileder(henvendelseMedId);
        }
    }

    void markerSomLestAvBruker(long dialogId) {
        settFeltTilNull(ELDSTE_ULESTE_FOR_BRUKER, dialogId);
    }

    void markerSomLestAvVeileder(long dialogId) {
        settFeltTilNull(ELDSTE_ULESTE_FOR_VEILEDER, dialogId);
    }

    void oppdaterVenterPaNav(long dialogId, boolean venter) {
        oppdaterVenterPaa(dialogId, venter, VENTER_PA_NAV_SIDEN);
    }

    void oppdaterVenterPaSvarFraBruker(long dialogId, boolean venter) {
        oppdaterVenterPaa(dialogId, venter, VENTER_PA_SVAR_FRA_BRUKER);
    }

    void oppdaterDialogTilHistorisk(DialogData dialogData) {
        database.update(settTilHistoriskSQL(), dialogData.getId());
    }

    private void nyHenvendelseFraVeileder(HenvendelseData henvendelesMedID) {
        oppdaterUlestTidspunkt(henvendelesMedID, ELDSTE_ULESTE_FOR_BRUKER);
    }

    private void nyHenvendelseFraBruker(HenvendelseData henvendelesMedID) {
        oppdaterUlestTidspunkt(henvendelesMedID, ELDSTE_ULESTE_FOR_VEILEDER);
        oppdaterVenterPaNav(henvendelesMedID.dialogId, true);
    }

    private void oppdaterVenterPaa(long dialogId, boolean venter, String feltnavn) {
        if (venter) {
            setNowIfNull(feltnavn, dialogId);
        } else {
            settFeltTilNull(feltnavn, dialogId);
        }
    }

    private void oppdaterUlestTidspunkt(HenvendelseData henvendelseData, String feltnavn) {
        int update = database.update("" +
                        "UPDATE DIALOG SET " +
                        "(" + feltnavn + ",  " + OPPDATERT + ")" +
                        " = " +
                        "(" +
                        "(SELECT SENDT FROM HENVENDELSE WHERE HENVENDELSE_ID = ?), " +
                        dateProvider.getNow() +
                        ") " +
                        "WHERE DIALOG_ID = ? " +
                        "AND " +
                        feltnavn + " IS NULL",
                henvendelseData.id, henvendelseData.dialogId
        );
        if (update == 0) {
            oppdaterSisteEndring(henvendelseData.dialogId);
        }
    }

    private void setNowIfNull(String feltnavn, long dialogId) {
        database.update("" +
                "UPDATE DIALOG SET " +
                feltnavn + " = " + dateProvider.getNow() + ", " +
                OPPDATERT + " = " + dateProvider.getNow() +
                " WHERE " + DIALOG_ID + " = ?  AND " + feltnavn + " IS NULL ", dialogId
        );
    }

    private String settTilHistoriskSQL() {
        return "UPDATE DIALOG SET " +
                HISTORISK + " = 1, " +
                OPPDATERT + " = " + dateProvider.getNow() + ", " +
                ELDSTE_ULESTE_FOR_BRUKER + " = null, " +
                ELDSTE_ULESTE_FOR_VEILEDER + " = null, " +
                VENTER_PA_SVAR_FRA_BRUKER + " = null, " +
                VENTER_PA_NAV_SIDEN + " = null" +
                " WHERE " + DIALOG_ID + " = ?";
    }

    private void settFeltTilNull(String feltNavn, long dialogId) {
        database.update("" +
                        "UPDATE DIALOG SET " + feltNavn + " = null, " +
                        OPPDATERT + " = " + dateProvider.getNow() +
                        " WHERE " + DIALOG_ID + " = ?  AND " + feltNavn + " IS NOT NULL",
                dialogId
        );
    }

    private void oppdaterSisteEndring(long dialogId) {
        database.update("" +
                "UPDATE DIALOG SET " +
                OPPDATERT + " = " + dateProvider.getNow() +
                " WHERE " + DIALOG_ID + " = ?", dialogId);
    }
}
