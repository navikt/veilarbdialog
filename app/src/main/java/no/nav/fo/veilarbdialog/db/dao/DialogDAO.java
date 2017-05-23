package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.db.Database;
import no.nav.fo.veilarbdialog.domain.*;
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

    private static final String SELECT_DIALOG = "SELECT * FROM DIALOG d LEFT JOIN DIALOG_STATUS s ON d.DIALOG_ID = s.DIALOG_ID ";

    @Inject
    private Database database;

    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return database.query(SELECT_DIALOG + "WHERE d.aktor_id = ?",
                this::mapTilDialog,
                aktorId
        );
    }

    public DialogData hentDialog(long dialogId) {
        return database.queryForObject(SELECT_DIALOG + "WHERE d.dialog_id = ?",
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
                .lestAvBrukerTidspunkt(hentDato(rs,"lest_av_bruker_tid"))
                .lestAvBruker(rs.getBoolean("lest_av_bruker"))
                .lestAvVeileder(rs.getBoolean("lest_av_veileder"))
                .venterPaSvar(rs.getBoolean("venter_pa_svar"))
                .ferdigbehandlet(rs.getBoolean("ferdigbehandlet"))
                .sisteStatusEndring(hentDato(rs, "siste_status_endring"))
                .henvendelser(hentHenvendelser(dialogId))  // TODO nøstet spørring, mulig at vi istede bør gjøre to spørringer og flette dataene
                .build();
    }

    private List<HenvendelseData> hentHenvendelser(long dialogId) {
        return database.query("SELECT * FROM HENVENDELSE h LEFT JOIN DIALOG d ON h.dialog_id = d.dialog_id WHERE h.dialog_id = ?",
                this::mapTilHenvendelse,
                dialogId
        );
    }

    private HenvendelseData mapTilHenvendelse(ResultSet rs) throws SQLException {
        Date henvendelseDato = hentDato(rs, "sendt");
        return HenvendelseData.builder()
                .dialogId(rs.getLong("dialog_id"))
                .sendt(henvendelseDato)
                .tekst(rs.getString("tekst"))
                .avsenderId(rs.getString("avsender_id"))
                .avsenderType(EnumUtils.valueOf(AvsenderType.class, rs.getString("avsender_type")))
                .lestAvBruker(erLest(hentDato(rs, "lest_av_bruker_tid"), henvendelseDato))
                .lestAvVeileder(erLest(hentDato(rs, "lest_av_veileder_tid"), henvendelseDato))
                .build();
    }

    private static boolean erLest(Date leseTidspunkt, Date henvendelseTidspunkt) {
        return leseTidspunkt != null && henvendelseTidspunkt.before(leseTidspunkt);
    }

    public long opprettDialog(DialogData dialogData) {
        long dialogId = database.nesteFraSekvens("DIALOG_ID_SEQ");
        database.update("INSERT INTO " +
                        "DIALOG (dialog_id,aktor_id,aktivitet_id,overskrift,lest_av_veileder_tid,lest_av_bruker_tid,siste_status_endring,skal_vente_pa_svar,markert_som_ferdigbehandlet) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)",
                dialogId,
                dialogData.aktorId,
                dialogData.aktivitetId,
                dialogData.overskrift,
                null,
                null,
                new Date(),
                false,
                false
        );
        LOG.info("opprettet {}", dialogData);
        return dialogId;
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
        markerLest(dialogId, "lest_av_veileder_tid");
    }

    public void markerDialogSomLestAvBruker(long dialogId) {
        markerLest(dialogId, "lest_av_bruker_tid");
    }

    private void markerLest(long dialogId, String feltNavn) {
        database.update("UPDATE DIALOG SET " + feltNavn + " = ? WHERE dialog_id = ? ",
                new Date(),
                dialogId
        );
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return database.query(SELECT_DIALOG + "WHERE aktivitet_id = ?", this::mapTilDialog, aktivitetId)
                .stream()
                .findFirst();
    }

    public List<DialogAktor> hentAktorerMedEndringerEtter(Date date) {
        return database.query("SELECT * FROM AKTOR_STATUS WHERE SISTE_ENDRING > ?", this::mapTilAktor, date);
    }

    private DialogAktor mapTilAktor(ResultSet resultSet) throws SQLException {
        return DialogAktor.builder()
                .aktorId(resultSet.getString("aktor_id"))
                .sisteEndring(hentDato(resultSet, "siste_endring"))
                .venterPaSvar(resultSet.getBoolean("venter_pa_svar"))
                .ubehandlet(!resultSet.getBoolean("ferdigbehandlet"))
                .build();
    }

    public void oppdaterDialogStatus(DialogStatus dialogStatus) {
        database.update("UPDATE DIALOG SET skal_vente_pa_svar = ?, markert_som_ferdigbehandlet = ?, siste_status_endring = ? WHERE dialog_id = ?",
                dialogStatus.venterPaSvar,
                dialogStatus.ferdigbehandlet,
                new Date(),
                dialogStatus.dialogId
        );
    }

}
