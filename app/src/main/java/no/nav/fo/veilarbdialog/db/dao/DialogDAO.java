package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.db.Database;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarbdialog.db.Database.hentDato;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DialogDAO {

    private static final Logger LOG = getLogger(DialogDAO.class);

    private static final String LEST_AV_VEILEDER = "lest_av_veileder";
    private static final String LEST_AV_BRUKER = "lest_av_bruker";

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
                .aktivitetId(rs.getString("aktivitet_id"))
                .overskrift(rs.getString("overskrift"))
                .lestAvBruker(hentDato(rs, LEST_AV_BRUKER))
                .lestAvVeileder(hentDato(rs, LEST_AV_VEILEDER))
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
                .sendt(hentDato(rs, "sendt"))
                .tekst(rs.getString("tekst"))
                .avsenderId(rs.getString("avsender_id"))
                .avsenderType(EnumUtils.valueOf(AvsenderType.class, rs.getString("avsender_type")))
                .build();
    }

    public DialogData opprettDialog(DialogData dialogData) {
        long dialogId = database.nesteFraSekvens("DIALOG_ID_SEQ");
        Date date = new Date();
        database.update("INSERT INTO DIALOG(dialog_id,aktor_id,aktivitet_id,overskrift,lest_av_veileder,lest_av_bruker) VALUES (?,?,?,?,?,?)",
                dialogId,
                dialogData.aktorId,
                dialogData.aktivitetId,
                dialogData.overskrift,
                date,
                date
        );
        LOG.info("opprettet {}", dialogData);
        return dialogData.toBuilder()
                .id(dialogId)
                .lestAvVeileder(date)
                .lestAvBruker(date)
                .build();
    }

    public void opprettHenvendelse(HenvendelseData henvendelseData) {
        database.update("INSERT INTO HENVENDELSE(dialog_id,sendt,tekst,avsender_id,avsender_type) VALUES (?,?,?,?,?)",
                henvendelseData.dialogId,
                new Date(),
                henvendelseData.tekst,
                henvendelseData.avsenderId,
                EnumUtils.getName(henvendelseData.avsenderType)
        );
        LOG.info("opprettet {}", henvendelseData);
    }

    public void markerDialogSomLestAvVeileder(long dialogId) {
        markerLest(dialogId, LEST_AV_VEILEDER);
    }

    public void markerDialogSomLestAvBruker(long dialogId) {
        markerLest(dialogId, LEST_AV_BRUKER);
    }

    private void markerLest(long dialogId, String feltNavn) {
        database.update("UPDATE DIALOG SET " + feltNavn + " = ? WHERE dialog_id = ? ",
                new Date(),
                dialogId
        );
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return database.query("SELECT * FROM DIALOG WHERE aktivitet_id = ?", this::mapTilDialog, aktivitetId)
                .stream()
                .findFirst();
    }

}
