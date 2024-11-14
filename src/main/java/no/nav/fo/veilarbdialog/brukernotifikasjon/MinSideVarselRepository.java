package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.MinSideVarselEntity;
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;
import no.nav.fo.veilarbdialog.util.DatabaseUtils;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MinSideVarselRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<MinSideVarselEntity> rowmapper = (rs, rowNum) ->
            new MinSideVarselEntity(
                    Optional.ofNullable(DatabaseUtils.hentMaybeUUID(rs, "event_id"))
                            .map(MinSideVarselId::new)
                            .orElse(null),
                    Fnr.of(rs.getString("foedselsnummer")),
                    DatabaseUtils.hentMaybeUUID(rs, "oppfolgingsperiode_id"),
                    EnumUtils.valueOf(BrukernotifikasjonsType.class, rs.getString("type")),
                    EnumUtils.valueOf(BrukernotifikasjonBehandlingStatus.class, rs.getString("status")),
                    EnumUtils.valueOf(VarselKvitteringStatus.class, rs.getString("varsel_kvittering_status")),
                    DatabaseUtils.hentLocalDateTime(rs, "opprettet"),
                    DatabaseUtils.hentLocalDateTime(rs, "varsel_feilet"),
                    DatabaseUtils.hentLocalDateTime(rs, "avsluttet"),
                    DatabaseUtils.hentLocalDateTime(rs, "bekreftet_sendt"),
                    DatabaseUtils.hentLocalDateTime(rs, "forsokt_sendt"),
                    DatabaseUtils.hentLocalDateTime(rs, "ferdig_behandlet"),
                    rs.getString("melding"),
                    DatabaseUtils.hentMaybeURL(rs, "lenke"),
                    rs.getBoolean("skal_batches")
            );

    MinSideVarselId opprettVarselIPendingStatus(DialogVarsel varsel) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("event_id", varsel.getVarselId().getValue().toString())
                .addValue("dialog_id", varsel.getDialogId())
                .addValue("foedselsnummer", varsel.getFoedselsnummer().get())
                .addValue("oppfolgingsperiode_id", varsel.getOppfolgingsperiodeId().toString())
                .addValue("type", varsel.getType().name())
                .addValue("status", BrukernotifikasjonBehandlingStatus.PENDING.name())
                .addValue("varsel_kvittering_status", VarselKvitteringStatus.IKKE_SATT.name())
                .addValue("melding", varsel.getMelding())
                .addValue("lenke", varsel.getLink().toExternalForm())
                .addValue("skalBatches", varsel.getSkalBatches());

        jdbcTemplate.update("" +
                        " INSERT INTO min_side_varsel " +
                        "        (varsel_id, foedselsnummer, oppfolgingsperiode_id, type, status, varsel_kvittering_status, opprettet, melding, lenke, skal_batches) " +
                        " VALUES (:varsel_id, :foedselsnummer, :oppfolgingsperiode_id, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :melding, :lenke, :skalBatches) ",
                params);

        return varsel.getVarselId();
    }

    Optional<BrukernotifikasjonEntity> hentBrukernotifikasjon(MinSideVarselId minSideVarselId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("varsel_id", minSideVarselId.getValue());
        String sql = """
                SELECT * FROM BRUKERNOTIFIKASJON WHERE ID = :id
                """;
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, params, rowmapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean finnesBrukernotifikasjon(MinSideVarselId varselId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("varlselId", varselId.getValue().toString());
        String sql = """
            SELECT COUNT(*) FROM BRUKERNOTIFIKASJON
            WHERE EVENT_ID=:varlselId
        """;
        int antall = jdbcTemplate.queryForObject(sql, params, int.class);
        return antall > 0;
    }

    public List<BrukernotifikasjonEntity> hentBrukernotifikasjonForDialogId(long dialogId, BrukernotifikasjonsType type) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dialogId", dialogId)
                .addValue("type", type.name());
        String sql = """
            SELECT *
            FROM BRUKERNOTIFIKASJON 
            WHERE DIALOG_ID = :dialogId AND
            TYPE = :type
            """;
        try {
            return jdbcTemplate.query(sql, params, rowmapper);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    List<BrukernotifikasjonEntity> hentPendingVarsler() {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("pending", BrukernotifikasjonBehandlingStatus.PENDING.name());
        String sql = """
                SELECT * FROM BRUKERNOTIFIKASJON WHERE STATUS = :pending
                """;
        return jdbcTemplate.query(sql, params, rowmapper);
    }

    List<BrukernotifikasjonEntity> hentVarslerSomSkalAvsluttes() {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("skal_avsluttes", BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES.name());
        String sql = """
            SELECT * FROM BRUKERNOTIFIKASJON WHERE STATUS = :skal_avsluttes
                """;
        return jdbcTemplate.query(sql, params, rowmapper);

    }

    void updateStatus(@NonNull MinSideVarselId varselId, @NonNull BrukernotifikasjonBehandlingStatus status) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", varselId.getValue().toString())
                .addValue("status", status.name());
        String sql = """
                UPDATE BRUKERNOTIFIKASJON SET STATUS = :status WHERE event_id = :id
                """;
        jdbcTemplate.update(sql, params);
    }

    public void setEksternVarselFeilet(MinSideVarselId varselId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("varlselId", varselId.getValue().toString())
                .addValue("varselKvitteringStatus", VarselKvitteringStatus.FEILET.toString());
        jdbcTemplate.update("""
             update BRUKERNOTIFIKASJON
               set
                VARSEL_FEILET = current_timestamp,
                VARSEL_KVITTERING_STATUS = :varselKvitteringStatus
                    where EVENT_ID = :varlselId
                 """, param);
    }

    public void setEksternVarselSendtOk(MinSideVarselId varlselId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
            .addValue("varlselId", varlselId.getValue().toString())
            .addValue("varselKvitteringStatusOk", VarselKvitteringStatus.OK.name());

        jdbcTemplate.update("""
            update BRUKERNOTIFIKASJON
            set
            BEKREFTET_SENDT = CURRENT_TIMESTAMP,
            VARSEL_KVITTERING_STATUS = :varselKvitteringStatusOk
            where EVENT_ID = :varlselId
            """, param);
    }


    void setSkalAvsluttesForVarslerIPeriode(UUID oppfolgingsperiodeUuid) {
        MapSqlParameterSource skalAvsluttes = new MapSqlParameterSource()
                .addValue("oppfolgingsperiode", oppfolgingsperiodeUuid.toString())
                .addValue("fra_status", BrukernotifikasjonBehandlingStatus.SENDT.name())
                .addValue("til_status", BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES.name());
        MapSqlParameterSource skalAvbrytes = new MapSqlParameterSource()
                .addValue("oppfolgingsperiode", oppfolgingsperiodeUuid.toString())
                .addValue("fra_status", BrukernotifikasjonBehandlingStatus.PENDING.name())
                .addValue("til_status", BrukernotifikasjonBehandlingStatus.AVSLUTTET.name());
        String sql = """
                UPDATE BRUKERNOTIFIKASJON SET STATUS = :til_status WHERE OPPFOLGINGSPERIODE_ID = :oppfolgingsperiode and status = :fra_status
                """;

        jdbcTemplate.update(sql, skalAvbrytes);
        jdbcTemplate.update(sql, skalAvsluttes);
    }
}
