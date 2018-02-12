package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import no.nav.sbl.jdbc.Database;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbdialog.db.dao.DBKonstanter.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Transactional
public class DialogDAO {

    private static final Logger LOG = getLogger(DialogDAO.class);

    private static final String SELECT_DIALOG = "SELECT * FROM DIALOG d ";

    private final Database database;
    private final DateProvider dateProvider;

    @Inject
    public DialogDAO(Database database, DateProvider dateProvider) {
        this.database = database;
        this.dateProvider = dateProvider;
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return database.query(SELECT_DIALOG + "WHERE d.aktor_id = ?",
                this::mapTilDialog,
                aktorId
        );
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerSomSkalAvsluttesForAktorId(String aktorId, Date avsluttetDato) {
        return database.query(SELECT_DIALOG + "WHERE d.aktor_id = ? AND historisk = 0 AND d.OPPRETTET_DATO < ?",
                this::mapTilDialog,
                aktorId,
                avsluttetDato
        );
    }

    @Transactional(readOnly = true)
    public DialogData hentDialog(long dialogId) {
        return database.queryForObject(SELECT_DIALOG + "WHERE d.dialog_id = ?",
                this::mapTilDialog,
                dialogId
        );
    }

    @Transactional(readOnly = true)
    public HenvendelseData hentHenvendelse(long id) {
        return database.query(
                "SELECT * FROM HENVENDELSE h LEFT JOIN DIALOG d ON h.dialog_id = d.dialog_id WHERE h.henvendelse_id = ?",
                this::mapTilHenvendelse,
                id).stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return database.query(SELECT_DIALOG + "WHERE aktivitet_id = ?", this::mapTilDialog, aktivitetId)
                .stream()
                .findFirst();
    }

    public DialogData opprettDialog(DialogData dialogData) {
        long dialogId = database.nesteFraSekvens("DIALOG_ID_SEQ");

        database.update("INSERT INTO " +
                        "DIALOG (dialog_id, " +
                        "aktor_id, " +
                        "opprettet_dato, " +
                        "aktivitet_id, " +
                        "overskrift, " +
                        "historisk, " +
                        "kontorsperre_enhet_id, " +
                        OPPDATERT + ", " +
                        "siste_status_endring) " + //TODO ved neste migrering slett denne
                        "VALUES (?,?," + dateProvider.getNow() + ",?,?,?,?," + dateProvider.getNow() + "," + dateProvider.getNow() + ")",
                dialogId,
                dialogData.getAktorId(),
                dialogData.getAktivitetId(),
                dialogData.getOverskrift(),
                dialogData.isHistorisk() ? 1 : 0,
                dialogData.getKontorsperreEnhetId()
        );

        dialogData.getEgenskaper().forEach(egenskapType ->
                database.update("INSERT INTO DIALOG_EGENSKAP(DIALOG_ID, DIALOG_EGENSKAP_TYPE_KODE) VALUES (?, ?)",
                        dialogId, egenskapType.toString())
        );

        LOG.info("opprettet dialog id:{} data:{}", dialogId, dialogData);
        return hentDialog(dialogId);
    }

    public long opprettHenvendelse(HenvendelseData henvendelseData) {
        long henvendelseId = database.nesteFraSekvens("HENVENDELSE_ID_SEQ");

        database.update("INSERT INTO HENVENDELSE(" +
                        "henvendelse_id, " +
                        "dialog_id, " +
                        "sendt, " +
                        "tekst, " +
                        "kontorsperre_enhet_id, " +
                        "avsender_id, " +
                        "avsender_type) " +
                        "VALUES (?,?,?,?,?,?,?)",
                henvendelseId,
                henvendelseData.dialogId,
                henvendelseData.sendt,
                henvendelseData.tekst,
                henvendelseData.kontorsperreEnhetId,
                henvendelseData.avsenderId,
                EnumUtils.getName(henvendelseData.avsenderType)
        );

        LOG.info("opprettet henvendelse id:{} data:{}", henvendelseId, henvendelseData);
        return henvendelseId;
    }

    public DialogData oppdaterStatus(Status status) {
        database.update("" +
                        "UPDATE DIALOG SET " +
                        VENTER_PA_NAV_SIDEN + " = ?, " +
                        VENTER_PA_SVAR_FRA_BRUKER + " = ?, " +
                        ELDSTE_ULESTE_FOR_BRUKER + " = ?, " +
                        ELDSTE_ULESTE_FOR_VEILEDER + " = ?, " +
                        LEST_AV_BRUKER_TID + " = ?, " +
                        LEST_AV_VEILEDER_TID + " = ?, " +
                        HISTORISK + " = ?, " +
                        OPPDATERT + " = " + dateProvider.getNow() + " " +
                        "WHERE " + DIALOG_ID + " = ?",
                status.getVenterPaNavSiden(),
                status.getVenterPaSvarFraBruker(),
                status.getEldsteUlesteForBruker(),
                status.getEldsteUlesteForVeileder(),
                status.getLestAvBrukerTid(),
                status.getLestAvVeilederTid(),
                status.getHistorisk(),
                status.getDialogId());

        return hentDialog(status.getDialogId());
    }

    private static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    private static boolean erLest(Date leseTidspunkt, Date henvendelseTidspunkt) {
        return leseTidspunkt != null && henvendelseTidspunkt.before(leseTidspunkt);
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
                .henvendelser(hentHenvendelser(dialogId))
                .historisk(rs.getBoolean("historisk"))
                .opprettetDato(hentDato(rs, "opprettet_dato"))
                .venterPaNavSiden(hentDato(rs, VENTER_PA_NAV_SIDEN))
                .venterPaSvarFraBrukerSiden(hentDato(rs, VENTER_PA_SVAR_FRA_BRUKER))
                .eldsteUlesteTidspunktForBruker(hentDato(rs, ELDSTE_ULESTE_FOR_BRUKER))
                .eldsteUlesteTidspunktForVeileder(hentDato(rs, ELDSTE_ULESTE_FOR_VEILEDER))
                .oppdatert(hentDato(rs, OPPDATERT))
                .kontorsperreEnhetId(rs.getString("kontorsperre_enhet_id"))
                .egenskaper(egenskaper)
                .build();
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
                .kontorsperreEnhetId(rs.getString("kontorsperre_enhet_id"))
                .build();
    }
}
