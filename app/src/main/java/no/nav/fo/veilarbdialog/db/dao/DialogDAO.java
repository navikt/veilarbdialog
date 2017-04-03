package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.db.Database;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbdialog.db.Database.hentDato;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DialogDAO {

    private static final Logger LOG = getLogger(DialogDAO.class);

    @Inject
    private Database database;

    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return database.query("SELECT * FROM DIALOG WHERE aktor_id = ?",
                this::mapTilDialog,
                aktorId
        );
    }

    public DialogData hentDialog(long dialogId) {
        return database.queryForObject("SELECT * FROM DIALOG WHERE dialog_id = ?",
                this::mapTilDialog,
                dialogId
        );
    }

    private DialogData mapTilDialog(ResultSet rs) throws SQLException {
        long dialogId = rs.getLong("dialog_id");
        return DialogData.builder()
                .id(dialogId)
                .aktorId(rs.getString("aktor_id"))
                .overskrift(rs.getString("overskrift"))
                .henvendelser(hentHenvendelser(dialogId)) // TODO nøstet spørring, mulig at vi istede bør gjøre to spørringer og flette dataene
                .build();
    }

    private List<HenvendelseData> hentHenvendelser(long dialogId) {
        return database.query("SELECT * FROM HENVENDELSE WHERE dialog_id = ?",
                this::mapTilHenvendelse,
                dialogId
        );
    }

    private HenvendelseData mapTilHenvendelse(ResultSet rs) throws SQLException {
        return HenvendelseData.builder()
                .dialogId(rs.getLong("dialog_id"))
                .sendt(hentDato(rs,"sendt"))
                .tekst(rs.getString("tekst"))
                .build();
    }

    public DialogData opprettDialog(DialogData dialogData) {
        long dialogId = database.nesteFraSekvens("DIALOG_ID_SEQ");
        database.update("INSERT INTO DIALOG(dialog_id,aktor_id,overskrift) VALUES (?,?,?)",
                dialogId,
                dialogData.aktorId,
                dialogData.overskrift
        );
        LOG.info("opprettet {}", dialogData);
        return dialogData.toBuilder()
                .id(dialogId)
                .build();
    }

    public void opprettHenvendelse(HenvendelseData henvendelseData) {
        database.update("INSERT INTO HENVENDELSE(dialog_id,sendt,tekst) VALUES (?,?,?)",
                henvendelseData.dialogId,
                new Date(),
                henvendelseData.tekst
        );
        LOG.info("opprettet {}", henvendelseData);
    }


}
