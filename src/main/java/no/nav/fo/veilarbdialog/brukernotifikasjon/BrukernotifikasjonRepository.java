package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
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

@Repository
@RequiredArgsConstructor
public class BrukernotifikasjonRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<BrukernotifikasjonEntity> rowmapper = (rs, rowNum) ->
            new BrukernotifikasjonEntity(
                    rs.getLong("id"),
                    DatabaseUtils.hentMaybeUUID(rs, "event_id"),
                    rs.getLong("dialog_id"),
                    Fnr.of(rs.getString("foedselsnummer")),
                    DatabaseUtils.hentMaybeUUID(rs, "oppfolgingsperiode_id"),
                    EnumUtils.valueOf(BrukernotifikasjonsType.class, rs.getString("type")),
                    EnumUtils.valueOf(BrukernotifikasjonBehandlingStatus.class, rs.getString("status")),
                    DatabaseUtils.hentLocalDateTime(rs, "opprettet"),
                    DatabaseUtils.hentLocalDateTime(rs, "forsokt_sendt"),
                    rs.getString("melding"),
                    rs.getString("smstekst"),
                    rs.getString("eposttittel"),
                    rs.getString("epostbody"),
                    DatabaseUtils.hentMaybeURL(rs, "lenke")
            );

    Long opprettBrukernotifikasjon(BrukernotifikasjonInsert insert) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("event_id", insert.eventId().toString())
                .addValue("dialog_id", insert.dialogId())
                .addValue("foedselsnummer", insert.foedselsnummer().get())
                .addValue("oppfolgingsperiode_id", insert.oppfolgingsperiodeId().toString())
                .addValue("type", insert.type().name())
                .addValue("status", insert.status().name())
                .addValue("varsel_kvittering_status", VarselKvitteringStatus.IKKE_SATT.name())
                .addValue("epostTittel", insert.epostTitel())
                .addValue("epostBody", insert.epostBody())
                .addValue("smsTekst", insert.smsTekst())
                .addValue("melding", insert.melding())
                .addValue("lenke", insert.link().toExternalForm());

        jdbcTemplate.update("" +
                        " INSERT INTO brukernotifikasjon " +
                        "        (event_id, DIALOG_ID, foedselsnummer, oppfolgingsperiode_id, type, status, varsel_kvittering_status, opprettet, melding, smsTekst,  epostTittel, epostBody, lenke) " +
                        " VALUES (:event_id, :dialog_id, :foedselsnummer, :oppfolgingsperiode_id, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :melding, :smsTekst, :epostTittel, :epostBody, :lenke) ",
                params, keyHolder, new String[]{"id"});

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new DataAccessResourceFailureException("Generated key not present");
        } else {
            return generatedKey.longValue();
        }
    }

    Optional<BrukernotifikasjonEntity> hentBrukernotifikasjon(long id) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);
        String sql = """
                SELECT * FROM BRUKERNOTIFIKASJON WHERE ID = :id
                """;
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, params, rowmapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<BrukernotifikasjonEntity> hentBrukernotifikasjonBeskjedForDialogId(long dialogId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dialogId", dialogId)
                .addValue("type", BrukernotifikasjonsType.BESKJED.name());
        String sql = """
            SELECT *
            FROM BRUKERNOTIFIKASJON 
            WHERE DIALOG_ID = :dialogId AND
            TYPE = :type
            """;
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, params, rowmapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    List<BrukernotifikasjonEntity> hentPendingBrukernotifikasjoner() {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("pending", BrukernotifikasjonBehandlingStatus.PENDING.name());
        String sql = """
                SELECT * FROM BRUKERNOTIFIKASJON WHERE STATUS = :pending
                """;
        return jdbcTemplate.query(sql, params, rowmapper);
    }

    List<BrukernotifikasjonEntity> hentPendingDoneBrukernotifikasjoner() {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("skal_avsluttes", BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES.name());
        String sql = """
            SELECT * FROM BRUKERNOTIFIKASJON WHERE STATUS = :skal_avsluttes
                """;
        return jdbcTemplate.query(sql, params, rowmapper);

    }

    void updateStatus(@NonNull Long id, @NonNull BrukernotifikasjonBehandlingStatus status) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status.name());
        String sql = """
                UPDATE BRUKERNOTIFIKASJON SET STATUS = :status WHERE id = :id
                """;
        jdbcTemplate.update(sql, params);
    }
}
