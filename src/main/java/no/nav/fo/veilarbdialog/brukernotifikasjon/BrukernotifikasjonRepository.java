package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import no.nav.fo.veilarbdialog.util.DatabaseUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrukernotifikasjonRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<BrukernotifikasjonEntity> rowmapper = (rs, rowNum) ->
            new BrukernotifikasjonEntity(
                    rs.getLong("id"),
                    DatabaseUtils.hentMaybeUUID(rs, "event_id"),
                    DatabaseUtils.hentMaybeUUID(rs, "oppfolgingsperiode_id")
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
                .addValue("melding", insert.melding());

        jdbcTemplate.update("" +
                        " INSERT INTO brukernotifikasjon " +
                        "        (event_id, DIALOG_ID, foedselsnummer, oppfolgingsperiode_id, type, status, varsel_kvittering_status, opprettet, melding, smsTekst,  epostTittel, epostBody) " +
                        " VALUES (:event_id, :dialog_id, :foedselsnummer, :oppfolgingsperiode_id, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :melding, :smsTekst, :epostTittel, :epostBody) ",
                params, keyHolder);

        return DatabaseUtils.getGeneratedKey(keyHolder);
    }

    public Optional<BrukernotifikasjonEntity> hentBrukernotifikasjon(long id) {
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

}
