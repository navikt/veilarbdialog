package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.db.Database;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Inject
    DateProvider dateProvider;

    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return database.query(SELECT_DIALOG + "WHERE d.aktor_id = ?",
                this::mapTilDialog,
                aktorId
        );
    }

    public List<DialogData> hentGjeldendeDialogerForAktorId(String aktorId) {
        return database.query(SELECT_DIALOG + "WHERE d.aktor_id = ? AND historisk = 0",
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
                .venterPaSvar(hentDato(rs, "vente_pa_svar_tid") != null)
                .ferdigbehandlet(
                        hentDato(rs, "siste_ferdigbehandlet_tid") != null && hentDato(rs, "eldste_ubehandlede_tid") == null
                )
                .sisteStatusEndring(hentDato(rs, "siste_status_endring"))
                .henvendelser(hentHenvendelser(dialogId))  // TODO nøstet spørring, mulig at vi istede bør gjøre to spørringer og flette dataene
                .historisk(rs.getBoolean("historisk"))
                .opprettetDato(hentDato(rs, "opprettet_dato"))
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
                        "DIALOG (dialog_id,aktor_id,opprettet_dato,aktivitet_id,overskrift,historisk,siste_status_endring) " +
                        "VALUES (?,?," + dateProvider.getNow() + ",?,?,?," + dateProvider.getNow() + ")",
                dialogId,
                dialogData.aktorId,
                dialogData.aktivitetId,
                dialogData.overskrift,
                dialogData.historisk ? 1 : 0
        );
        LOG.info("opprettet {}", dialogData);
        return dialogId;
    }

    public void opprettHenvendelse(HenvendelseData henvendelseData) {
        database.update("INSERT INTO HENVENDELSE(dialog_id,sendt,tekst,avsender_id,avsender_type) VALUES (?," + dateProvider.getNow() + ",?,?,?)",
                henvendelseData.dialogId,
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
        database.update("UPDATE DIALOG SET " + feltNavn + " = " + dateProvider.getNow() + " WHERE dialog_id = ? ",
                dialogId
        );
    }

    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return database.query(SELECT_DIALOG + "WHERE aktivitet_id = ?", this::mapTilDialog, aktivitetId)
                .stream()
                .findFirst();
    }

    public List<DialogAktor> hentAktorerMedEndringerFOM(Date date) {
        return database.query("SELECT * FROM AKTOR_STATUS WHERE SISTE_ENDRING >= ?", this::mapTilAktor, date);
    }

    private DialogAktor mapTilAktor(ResultSet resultSet) throws SQLException {
        return DialogAktor.builder()
                .aktorId(resultSet.getString("aktor_id"))
                .sisteEndring(hentDato(resultSet, "siste_endring"))
                .tidspunktEldsteUbehandlede(hentDato(resultSet,"tidspunkt_eldste_ubehandlede"))
                .tidspunktEldsteVentende(hentDato(resultSet,"tidspunkt_eldste_ventende"))
                .build();
    }

    public void oppdaterDialogStatus(DialogStatus dialogStatus) {
        database.update("UPDATE DIALOG SET " +
                        "siste_vente_pa_svar_tid = " + nowOrNull(dialogStatus.venterPaSvar) + ", " +
                        "siste_ferdigbehandlet_tid =  " + nowOrNull(dialogStatus.ferdigbehandlet) + ", " +
                        "siste_status_endring = " + dateProvider.getNow() + " " +
                        "WHERE dialog_id = ?",
                dialogStatus.dialogId
        );
    }

    @Transactional
    public void settDialogTilHistoriskOgOppdaterFeed(DialogData dialog) {
        database.update("UPDATE DIALOG SET " +
                        "historisk = 1 "+
                        "WHERE dialog_id = ? AND " +
                        "opprettet_dato < ?",
                dialog.id,
                dialog.getOpprettetDato()
        );
        oppdaterFeed();
    }

    private void oppdaterFeed() {
        database.update("UPDATE FEED_METADATA SET " +
                "tidspunkt_siste_endring = " + dateProvider.getNow()
        );
    }

    private String nowOrNull(boolean venterPaSvar) {
        return venterPaSvar ? dateProvider.getNow() : "NULL";
    }

}
