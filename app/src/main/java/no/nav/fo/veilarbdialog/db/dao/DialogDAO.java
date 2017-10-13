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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
//import static no.nav.fo.veilarbdialog.db.Database.hentDato;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DialogDAO {

    private static final Logger LOG = getLogger(DialogDAO.class);

    private static final String SELECT_DIALOG = "SELECT * FROM DIALOG d ";

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
                        "DIALOG (dialog_id,aktor_id,opprettet_dato,aktivitet_id,overskrift,historisk,siste_status_endring) " +
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

        LOG.info("opprettet {}", dialogData);
        return dialogId;
    }

    public void opprettHenvendelse(HenvendelseData henvendelseData) {
        long henvendeseId = database.nesteFraSekvens("HENVENDELSE_ID_SEQ");
        database.update("INSERT INTO HENVENDELSE(henvendelse_id,dialog_id,sendt,tekst,avsender_id,avsender_type) VALUES (?,?," + dateProvider.getNow() + ",?,?,?)",
                henvendeseId,
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

    public List<DialogAktor> hentAktorerMedEndringerFOM(Date date, int pageSize) {
        return hentDialogerMedEndringerFOM(date, pageSize)
                .stream()
                .collect(groupingBy(DialogData::getAktorId))
                .entrySet()
                .stream()
                .map(this::mapTilAktor)
                .collect(toList());
    }

    private static final String DIALOGER_ENDRET_ETTER_DATO = "SELECT GREATEST(d.siste_status_endring, h.sendt) as siste_endring, d.* "
            + "FROM DIALOG d "
            + "LEFT JOIN HENVENDELSE h ON h.dialog_id = d.dialog_id "
            + "WHERE SISTE_STATUS_ENDRING >= ? OR h.sendt >= ? "
            + "ORDER BY siste_endring "
            + "FETCH FIRST ? ROWS ONLY";
    
    private static final String AKTORID_FOR_AKTORER_SOM_HAR_ENDREDE_DIALOGER = "SELECT DISTINCT AKTOR_ID FROM (" + DIALOGER_ENDRET_ETTER_DATO + ")";

    private List<DialogData> hentDialogerMedEndringerFOM(Date date, int pageSize) {

        List<String> aktorIder = database.query(
                AKTORID_FOR_AKTORER_SOM_HAR_ENDREDE_DIALOGER,
                resultSet -> resultSet.getString("AKTOR_ID"),
                date,
                date,
                pageSize);

        return hentDialogerForAktorIder(aktorIder);

    }
    
    private List<DialogData> hentDialogerForAktorIder(Collection<String> aktorIder) {
        return (aktorIder == null || aktorIder.isEmpty()) ? Collections.emptyList() : 
            database.queryWithNamedParam(SELECT_DIALOG + "WHERE d.aktor_id in ( :aktorer )",
                    this::mapTilDialog,
                    Collections.singletonMap("aktorer", aktorIder)
      );
    }


    private DialogAktor mapTilAktor(Map.Entry<String, List<DialogData>> dialogerForAktorId) {
        List<DialogData> dialogData = dialogerForAktorId.getValue();
        return DialogAktor.builder()
                .aktorId(dialogerForAktorId.getKey())
                .sisteEndring(dialogData.stream()
                        .map(DialogData::getSisteEndring)
                        .max(naturalOrder())
                        .orElse(null)
                )
                .tidspunktEldsteVentende(dialogData.stream()
                        .filter(DialogData::venterPaSvar)
                        .map(DialogData::getVenterPaSvarTidspunkt)
                        .min(naturalOrder())
                        .orElse(null)
                )
                .tidspunktEldsteUbehandlede(dialogData.stream()
                        .filter(DialogData::erUbehandlet)
                        .map(DialogData::getUbehandletTidspunkt)
                        .min(naturalOrder())
                        .orElse(null)
                ).build();
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

    @Transactional
    public void settDialogTilHistoriskOgOppdaterFeed(DialogData dialog) {
        database.update("UPDATE DIALOG SET " +
                        "historisk = 1 " +
                        "WHERE dialog_id = ?",
                dialog.getId()
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
