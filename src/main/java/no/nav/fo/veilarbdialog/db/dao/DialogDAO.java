package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.EgenskapType;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbdialog.db.dao.DBKonstanter.*;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DialogDAO {

    private final JdbcTemplate jdbc;
    private final DateProvider dateProvider;

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return jdbc.query("select * from DIALOG where aktor_id = ?",
                new Object[]{aktorId},
                new MapTilDialog());
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(String aktorId, Date avsluttetDato) {
        return jdbc.query("select * from DIALOG where " +
                        "aktor_id = ? and " +
                        "historisk = 0 and " +
                        "OPPRETTET_DATO < ? and " +
                        "KONTORSPERRE_ENHET_ID is not null",
                new Object[]{
                        aktorId,
                        avsluttetDato
                },
                new MapTilDialog());
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerSomSkalAvsluttesForAktorId(String aktorId, Date avsluttetDato) {
        return jdbc.query("select * from DIALOG where " +
                        "aktor_id = ? and " +
                        "historisk = 0 and " +
                        "OPPRETTET_DATO < ?",
                new Object[]{
                        aktorId,
                        avsluttetDato
                },
                new MapTilDialog());
    }

    @Transactional(readOnly = true)
    public DialogData hentDialog(long dialogId) {
        return jdbc.queryForObject("select * from DIALOG where dialog_id = ?",
                new Object[]{dialogId},
                new MapTilDialog());
    }

    @Transactional(readOnly = true)
    public DialogData hentDialogGittHenvendelse(long henvendelseId) {
        return jdbc.queryForObject("select d.* from DIALOG d " +
                        "left join HENVENDELSE h on h.dialog_id = d.dialog_id " +
                        "where h.henvendelse_id = ?",
                new Object[]{henvendelseId},
                new MapTilDialog());
    }

    @Transactional(readOnly = true)
    public HenvendelseData hentHenvendelse(long id) {
        return jdbc.query("select * from HENVENDELSE h " +
                        "left join DIALOG d on d.dialog_id = h.dialog_id " +
                        "where h.henvendelse_id = ?",
                new Object[]{id},
                new MapTilHenvendelse())
                .stream()
                .findFirst()
                .orElse(null);
    }

    public int kasserHenvendelse(long id) {
        return jdbc.update("update HENVENDELSE set TEKST = 'Kassert av NAV' where HENVENDELSE_ID = ?",
                id);
    }

    public int kasserDialog(long id) {
        return jdbc.update("update DIALOG set OVERSKRIFT = 'Kassert av NAV' where DIALOG_ID = ?",
                id);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
        return jdbc.query("select * from DIALOG where aktivitet_id = ?",
                new Object[]{aktivitetId},
                new MapTilDialog())
                .stream()
                .findFirst();
    }

    public DialogData opprettDialog(DialogData dialogData) {
        long dialogId = Optional
                .ofNullable(jdbc.queryForObject("select DIALOG_ID_SEQ.nextval from dual", Long.class))
                .orElseThrow(IllegalStateException::new);

        jdbc.update("insert into DIALOG (dialog_id, aktor_id, opprettet_dato, aktivitet_id, overskrift, historisk, kontorsperre_enhet_id, oppdatert) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?)",
                dialogId,
                dialogData.getAktorId(),
                dateProvider.getNow(),
                dialogData.getAktivitetId(),
                dialogData.getOverskrift(),
                dialogData.isHistorisk() ? 1 : 0,
                dialogData.getKontorsperreEnhetId(),
                dateProvider.getNow());

        dialogData.getEgenskaper()
                .forEach(egenskapType -> updateDialogEgenskap(egenskapType, dialogId));

        log.info("opprettet dialog id:{} data:{}", dialogId, dialogData);
        return hentDialog(dialogId);
    }

    public void updateDialogEgenskap(EgenskapType type, long dialogId) {
        jdbc.update("insert into DIALOG_EGENSKAP (DIALOG_ID, DIALOG_EGENSKAP_TYPE_KODE) " +
                        "values (?, ?)",
                dialogId,
                type.toString());
    }

    public HenvendelseData opprettHenvendelse(HenvendelseData henvendelseData) {
        long henvendelseId = Optional
                .ofNullable(jdbc.queryForObject("select HENVENDELSE_ID_SEQ.nextval from dual", Long.class))
                .orElseThrow(IllegalStateException::new);

        jdbc.update("insert into HENVENDELSE (henvendelse_id, dialog_id, sendt, tekst, kontorsperre_enhet_id, avsender_id, avsender_type, viktig) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?)",
                henvendelseId,
                henvendelseData.dialogId,
                dateProvider.getNow(),
                henvendelseData.tekst,
                henvendelseData.kontorsperreEnhetId,
                henvendelseData.avsenderId,
                EnumUtils.getName(henvendelseData.avsenderType),
                henvendelseData.viktig);

        log.info("opprettet henvendelse id:{} data:{}", henvendelseId, henvendelseData);
        return hentHenvendelse(henvendelseId);
    }

    private static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    private static boolean erLest(Date eldsteUleste, Date henvendelseTidspunkt) {
        return eldsteUleste == null || henvendelseTidspunkt.before(eldsteUleste);
    }

    private List<HenvendelseData> hentHenvendelser(long dialogId) {
        return jdbc.query("select * from HENVENDELSE h " +
                        "left join DIALOG d on d.dialog_id = h.dialog_id " +
                        "where h.dialog_id = ?",
                new Object[]{dialogId},
                new MapTilHenvendelse());
    }

    private class MapTilDialog implements RowMapper<DialogData> {

        @Override
        public DialogData mapRow(ResultSet rs, int rowNum) throws SQLException {

            long dialogId = rs.getLong("dialog_id");
            List<EgenskapType> egenskaper =
                    jdbc.query("select * from DIALOG_EGENSKAP where dialog_id = ?",
                            new Object[]{dialogId},
                            (rsInner, rowNumInner) -> Optional
                                    .ofNullable(rs.getString("DIALOG_EGENSKAP_TYPE_KODE"))
                                    .map(EgenskapType::valueOf)
                                    .orElse(null));
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
                    .harUlestParagraf8Henvendelse(rs.getBoolean(HAR_ULEST_PARAGRAF_8))
                    .paragraf8VarselUUID(rs.getString(PARAGRAF8_VARSEL_UUID))
                    .build();

        }

    }

    private static class MapTilHenvendelse implements RowMapper<HenvendelseData> {

        @Override
        public HenvendelseData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Date henvendelseDato = hentDato(rs, "sendt");
            return HenvendelseData.builder()
                    .id(rs.getLong("henvendelse_id"))
                    .dialogId(rs.getLong("dialog_id"))
                    .sendt(henvendelseDato)
                    .tekst(rs.getString("tekst"))
                    .avsenderId(rs.getString("avsender_id"))
                    .avsenderType(EnumUtils.valueOf(AvsenderType.class, rs.getString("avsender_type")))
                    .lestAvBruker(erLest(hentDato(rs, ELDSTE_ULESTE_FOR_BRUKER), henvendelseDato))
                    .lestAvVeileder(erLest(hentDato(rs, ELDSTE_ULESTE_FOR_VEILEDER), henvendelseDato))
                    .kontorsperreEnhetId(rs.getString("kontorsperre_enhet_id"))
                    .viktig(rs.getBoolean("viktig"))
                    .build();
        }

    }

}
