package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DialogDAO {

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return jdbc.query("select * from DIALOG where AKTOR_ID = ?",
                new MapTilDialog(),
                aktorId);
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForOppfolgingsperiodeId(UUID oppfolgingsperiodeId) {
        return jdbc.query("select * from DIALOG where OPPFOLGINGSPERIODE_UUID = ?",
                new MapTilDialog(),
                oppfolgingsperiodeId.toString());
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(String aktorId, Date avsluttetDato) {
        return jdbc.query("select * from DIALOG where " +
                        "AKTOR_ID = ? and " +
                        "HISTORISK = 0 and " +
                        "OPPRETTET_DATO < ? and " +
                        "KONTORSPERRE_ENHET_ID is not null",
                new MapTilDialog(),
                aktorId,
                avsluttetDato);
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerSomSkalAvsluttesForAktorId(String aktorId, Date avsluttetDato) {
        return jdbc.query("select * from DIALOG where " +
                        "AKTOR_ID = ? and " +
                        "HISTORISK = 0 and " +
                        "OPPRETTET_DATO < ?",
                new MapTilDialog(),
                aktorId,
                avsluttetDato);
    }

    @Transactional(readOnly = true)
    public DialogData hentDialog(long dialogId) {
        try {
            return jdbc.queryForObject("select * from DIALOG where DIALOG_ID = ?",
                    new MapTilDialog(),
                    dialogId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public DialogData hentDialogGittHenvendelse(long henvendelseId) {
        try {
            return jdbc.queryForObject("select d.* from DIALOG d " +
                            "left join HENVENDELSE h on h.DIALOG_ID = d.DIALOG_ID " +
                            "where h.HENVENDELSE_ID = ?",
                    new MapTilDialog(),
                    henvendelseId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public HenvendelseData hentHenvendelse(long id) {
        return jdbc.query("select * from HENVENDELSE h " +
                        "left join DIALOG d on d.DIALOG_ID = h.DIALOG_ID " +
                        "where h.HENVENDELSE_ID = ?",
                new MapTilHenvendelse(),
                id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public int kasserHenvendelse(long id) {
        return jdbc.update("update HENVENDELSE set TEKST = '- Det var skrevet noe feil, og det er nå slettet. -' where HENVENDELSE_ID = ?",
                id);
    }

    public int kasserDialog(long id) {
        return jdbc.update("update DIALOG set OVERSKRIFT = '- Det var skrevet noe feil, og det er nå slettet. -' where DIALOG_ID = ?",
                id);
    }

    private String getIdQuery(AktivitetId aktivitetId) {
        if (aktivitetId instanceof TekniskId) {
            return "select * from DIALOG where AKTIVITET_ID = ?";
        } else if (aktivitetId instanceof Arenaid) {
            return "select * from DIALOG where ARENA_ID = ?";
        } else {
            throw new UnsupportedOperationException("Uknown id-type");
        }
    }
    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(AktivitetId aktivitetId) {
        if (aktivitetId == null) return Optional.empty();
        return jdbc.query(getIdQuery(aktivitetId),
                new MapTilDialog(),
                aktivitetId.getId())
                .stream()
                .findFirst();
    }

    public DialogData opprettDialog(DialogData dialogData) {
        long dialogId = Optional
                .ofNullable(jdbc.queryForObject("select nextval('DIALOG_ID_SEQ')", Long.class))
                .orElseThrow(IllegalStateException::new);

        var hasId = dialogData.getAktivitetId() != null;
        var isTekniskId = dialogData.getAktivitetId() instanceof TekniskId;
        var arenaId = hasId && !isTekniskId  ? dialogData.getAktivitetId().getId() : null;
        var tekniskId = hasId && isTekniskId ? dialogData.getAktivitetId().getId() : null;

        jdbc.update("insert into DIALOG (DIALOG_ID, AKTOR_ID, OPPRETTET_DATO, AKTIVITET_ID, ARENA_ID, OVERSKRIFT, HISTORISK, KONTORSPERRE_ENHET_ID, OPPDATERT, OPPFOLGINGSPERIODE_UUID) " +
                        "values (?, ?, ? , ?, ?, ?, ?, ?, ?, ?)",
                dialogId,
                dialogData.getAktorId(),
                dialogData.getOpprettetDato(),
                tekniskId,
                arenaId,
                dialogData.getOverskrift(),
                dialogData.isHistorisk() ? 1 : 0,
                dialogData.getKontorsperreEnhetId(),
                dialogData.getOpprettetDato(),
                dialogData.getOppfolgingsperiode().toString()
        );

        dialogData.getEgenskaper()
                .forEach(egenskapType -> updateDialogEgenskap(egenskapType, dialogId));

        log.info("opprettet dialog id:{}", dialogId);
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
                .ofNullable(jdbc.queryForObject("select nextval('HENVENDELSE_ID_SEQ')", Long.class))
                .orElseThrow(IllegalStateException::new);

        jdbc.update("insert into HENVENDELSE (HENVENDELSE_ID, DIALOG_ID, SENDT, TEKST, KONTORSPERRE_ENHET_ID, AVSENDER_ID, AVSENDER_TYPE, VIKTIG) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?)",
                henvendelseId,
                henvendelseData.dialogId,
                henvendelseData.sendt,
                henvendelseData.tekst,
                henvendelseData.kontorsperreEnhetId,
                henvendelseData.avsenderId,
                EnumUtils.getName(henvendelseData.avsenderType),
                henvendelseData.viktig);

        log.info("opprettet henvendelse id:{} data:{}", henvendelseId, henvendelseData);
        return hentHenvendelse(henvendelseId);
    }

    @Transactional(readOnly = true)
    public List<String> hentAktorIderTilBrukereMedAktiveDialoger() {
        return jdbc.queryForList("select distinct AKTOR_ID from DIALOG where HISTORISK = 0", String.class);
    }

    private static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    private static UUID hentMaybeUUID(ResultSet rs, String kolonneNavn) throws SQLException {
        String uuid = rs.getString(kolonneNavn);

        if (StringUtils.isEmpty(uuid)) {
            return null;
        }

        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return  null;
        }
    }

    private class MapTilDialog implements RowMapper<DialogData> {

        @Override
        public DialogData mapRow(ResultSet rs, int rowNum) throws SQLException {

            var dialogId = rs.getLong("DIALOG_ID");
            List<EgenskapType> egenskaper =
                    jdbc.query("select d.DIALOG_EGENSKAP_TYPE_KODE from DIALOG_EGENSKAP d where d.DIALOG_ID = ?",
                            (rsInner, rowNumInner) -> Optional
                                    .ofNullable(rsInner.getString("DIALOG_EGENSKAP_TYPE_KODE"))
                                    .map(EgenskapType::valueOf)
                                    .orElse(null),
                            dialogId);
            var aktivitetId = Optional.ofNullable(rs.getString("AKTIVITET_ID"))
                    .orElse(rs.getString("ARENA_ID"));

            return DialogData.builder()
                    .id(dialogId)
                    .aktorId(rs.getString("AKTOR_ID"))
                    .aktivitetId(AktivitetId.of(aktivitetId))
                    .overskrift(rs.getString("OVERSKRIFT"))
                    .lestAvBrukerTidspunkt(hentDato(rs, "LEST_AV_BRUKER_TID"))
                    .lestAvVeilederTidspunkt(hentDato(rs, "LEST_AV_VEILEDER_TID"))
                    .henvendelser(hentHenvendelser(dialogId))
                    .historisk(rs.getBoolean("HISTORISK"))
                    .opprettetDato(hentDato(rs, "OPPRETTET_DATO"))
                    .venterPaNavSiden(hentDato(rs, "VENTER_PA_NAV_SIDEN"))
                    .venterPaSvarFraBrukerSiden(hentDato(rs, "VENTER_PA_SVAR_FRA_BRUKER"))
                    .eldsteUlesteTidspunktForBruker(hentDato(rs, "ELDSTE_ULESTE_FOR_BRUKER"))
                    .sisteUlestAvVeilederTidspunkt(hentDato(rs, "ELDSTE_ULESTE_FOR_VEILEDER"))
                    .oppdatert(hentDato(rs, "OPPDATERT"))
                    .kontorsperreEnhetId(rs.getString("KONTORSPERRE_ENHET_ID"))
                    .egenskaper(egenskaper)
                    .harUlestParagraf8Henvendelse(rs.getBoolean("ULESTPARAGRAF8VARSEL"))
                    .paragraf8VarselUUID(rs.getString("PARAGRAF8_VARSEL_UUID"))
                    .oppfolgingsperiode(hentMaybeUUID(rs, "OPPFOLGINGSPERIODE_UUID"))
                    .build();

        }

        private List<HenvendelseData> hentHenvendelser(long dialogId) {
            return jdbc.query("select * from HENVENDELSE h " +
                            "left join DIALOG d on d.DIALOG_ID = h.DIALOG_ID " +
                            "where h.DIALOG_ID = ?",
                    new MapTilHenvendelse(),
                    dialogId);
        }

    }

    private static class MapTilHenvendelse implements RowMapper<HenvendelseData> {

        @Override
        public HenvendelseData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Date henvendelseDato = hentDato(rs, "SENDT");
            return HenvendelseData.builder()
                    .id(rs.getLong("HENVENDELSE_ID"))
                    .dialogId(rs.getLong("DIALOG_ID"))
                    .sendt(henvendelseDato)
                    .tekst(rs.getString("TEKST"))
                    .avsenderId(rs.getString("AVSENDER_ID"))
                    .avsenderType(EnumUtils.valueOf(AvsenderType.class, rs.getString("AVSENDER_TYPE")))
                    .lestAvBruker(erLest(hentDato(rs, "ELDSTE_ULESTE_FOR_BRUKER"), henvendelseDato))
                    .lestAvVeileder(erLest(hentDato(rs, "ELDSTE_ULESTE_FOR_VEILEDER"), henvendelseDato))
                    .kontorsperreEnhetId(rs.getString("KONTORSPERRE_ENHET_ID"))
                    .viktig(rs.getBoolean("VIKTIG"))
                    .build();
        }

        private static boolean erLest(Date eldsteUleste, Date henvendelseTidspunkt) {
            return eldsteUleste == null || henvendelseTidspunkt.before(eldsteUleste);
        }

    }

}
