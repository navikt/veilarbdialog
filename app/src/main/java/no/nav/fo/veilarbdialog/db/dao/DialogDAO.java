package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import no.nav.sbl.jdbc.Database;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DialogDAO {

    private static final Logger LOG = getLogger(DialogDAO.class);

    private static final String SELECT_DIALOG = "SELECT * FROM DIALOG d ";

    private final Database database;
    private final DateProvider dateProvider;

    @Inject
    public DialogDAO(Database database,
                     DateProvider dateProvider) {
        this.database = database;
        this.dateProvider = dateProvider;
    }

    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return database.query(SELECT_DIALOG + "WHERE d.aktor_id = ?",
                this::mapTilDialog,
                aktorId
        );
    }

    public List<DialogData> hentDialogerSomSkalAvsluttesForAktorId(String aktorId, Date avsluttetDato) {
        return database.query(SELECT_DIALOG + "WHERE d.aktor_id = ? AND historisk = 0 AND d.OPPRETTET_DATO < ?",
                this::mapTilDialog,
                aktorId,
                avsluttetDato
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

        List<EgenskapType> egenskaper =
                database.query("SELECT * FROM DIALOG_EGENSKAP where dialog_id = ?",
                        this::mapTilEgenskap,
                        dialogId);

        return DialogData.builder()
                .id(dialogId)
                .aktorId(rs.getString("aktor_id"))
                .aktivitetId(rs.getString("aktivitet_id"))
                .overskrift(rs.getString("overskrift"))
                .lestAvBrukerTidspunkt(hentDato(rs, "lest_av_bruker_tid"))
                .lestAvVeilederTidspunkt(hentDato(rs, "lest_av_veileder_tid"))
                .venterPaSvarTidspunkt(hentDato(rs, "siste_vente_pa_svar_tid"))
                .ferdigbehandletTidspunkt(hentDato(rs, "siste_ferdigbehandlet_tid"))
                .ubehandletTidspunkt(hentDato(rs, "siste_ubehandlet_tid"))
                .sisteStatusEndring(hentDato(rs, "siste_status_endring"))
                .henvendelser(hentHenvendelser(dialogId))
                .historisk(rs.getBoolean("historisk"))
                .opprettetDato(hentDato(rs, "opprettet_dato"))
                .egenskaper(egenskaper)
                .build();
    }

    public static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn)).map(Timestamp::getTime).map(Date::new).orElse(null);
    }

    @SneakyThrows
    private EgenskapType mapTilEgenskap(ResultSet rs) {
        return Optional.ofNullable(rs.getString("DIALOG_EGENSKAP_TYPE_KODE")).map(EgenskapType::valueOf).orElse(null);
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
                .id(rs.getLong("henvendelse_id"))
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
                        "DIALOG (dialog_id, " +
                        "aktor_id, " +
                        "opprettet_dato, " +
                        "aktivitet_id, " +
                        "overskrift, " +
                        "historisk, " +
                        "siste_status_endring) " +
                        "VALUES (?,?," + dateProvider.getNow() + ",?,?,?," + dateProvider.getNow() + ")",
                dialogId,
                dialogData.getAktorId(),
                dialogData.getAktivitetId(),
                dialogData.getOverskrift(),
                dialogData.isHistorisk() ? 1 : 0
        );

        dialogData.getEgenskaper().forEach(egenskapType ->
                database.update("INSERT INTO DIALOG_EGENSKAP(DIALOG_ID, DIALOG_EGENSKAP_TYPE_KODE) VALUES (?, ?)",
                        dialogId, egenskapType.toString())
        );

        LOG.info("opprettet id:{}, data: {}", dialogId, dialogData);
        return dialogId;
    }

    public void opprettHenvendelse(HenvendelseData henvendelseData) {
        long henvendeseId = database.nesteFraSekvens("HENVENDELSE_ID_SEQ");

        database.update("INSERT INTO HENVENDELSE(" +
                        "henvendelse_id, " +
                        "dialog_id,sendt, " +
                        "tekst, " +
                        "avsender_id, " +
                        "avsender_type) " +
                        "VALUES (?,?," + dateProvider.getNow() + ",?,?,?)",
                henvendeseId,
                henvendelseData.dialogId,
                henvendelseData.tekst,
                henvendelseData.avsenderId,
                EnumUtils.getName(henvendelseData.avsenderType)
        );

        logNyHenvendelse(henvendelseData, henvendeseId);
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

    public void oppdaterDialogTilHistorisk(DialogData dialogData) {
        String sql = "UPDATE DIALOG SET " +
                "historisk = 1";

        if (dialogData.erUbehandlet()) {
            sql += ", siste_ferdigbehandlet_tid = " + dateProvider.getNow() +
                    ", siste_ubehandlet_tid = NULL";
        }
        if (dialogData.venterPaSvar()) {
            sql += ", siste_vente_pa_svar_tid = NULL";
        }

        if (dialogData.erUbehandlet() || dialogData.venterPaSvar()) {
            sql += ", siste_status_endring = " + dateProvider.getNow();
        }

        sql += " WHERE dialog_id = ?";

        database.update(sql, dialogData.getId());
    }

    public void oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        database.update("UPDATE DIALOG SET " +
                        "siste_ferdigbehandlet_tid =  " + nowOrNull(dialogStatus.ferdigbehandlet) + ", " +
                        "siste_ubehandlet_tid =  " + nowOrNull(!dialogStatus.ferdigbehandlet) + ", " +
                        "siste_status_endring = " + dateProvider.getNow() + " " +
                        "WHERE dialog_id = ?",
                dialogStatus.dialogId
        );
    }

    public void oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        database.update("UPDATE DIALOG SET " +
                        "siste_vente_pa_svar_tid = " + nowOrNull(dialogStatus.venterPaSvar) + ", " +
                        "siste_status_endring = " + dateProvider.getNow() + " " +
                        "WHERE dialog_id = ?",
                dialogStatus.dialogId
        );
    }

    private String nowOrNull(boolean predicate) {
        return predicate ? dateProvider.getNow() : "NULL";
    }


    private void logNyHenvendelse(HenvendelseData henvendelseData, long henvendeseId) {
        LOG.info("opprettet henvendelse ID: {}, dialogID: {}, avsenderType: {}, avsenderID: {}",
                henvendeseId,
                henvendelseData.dialogId,
                henvendelseData.avsenderType,
                henvendelseData.avsenderId
        );
    }
}
