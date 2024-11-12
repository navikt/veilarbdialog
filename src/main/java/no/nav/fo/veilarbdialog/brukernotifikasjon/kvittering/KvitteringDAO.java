package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import lombok.RequiredArgsConstructor;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KvitteringDAO {
    private final NamedParameterJdbcTemplate jdbc;

    private static final RowMapper<Kvittering> rowMapper = (rs, rowNum) -> new Kvittering(
            rs.getTimestamp("TIDSPUNKT").toLocalDateTime(),
            rs.getString("BRUKERNOTIFIKASJON_BESTILLING_ID"),
            rs.getString("DOKNOTIFIKASJON_STATUS"),
            rs.getString("MELDING"),
            rs.getLong("DISTRIBUSJON_ID")
    );

    public void lagreKvittering(MinSideVarselId varselId, DoknotifikasjonStatus melding) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_bestilling_id", varselId.getValue())
                .addValue("doknotifikasjon_status", melding.getStatus())
                .addValue("melding", melding.getMelding())
                .addValue("distribusjon_id", melding.getDistribusjonId())
                .addValue("json_payload",melding.toString());
        jdbc.update("""
                insert into  EKSTERN_VARSEL_KVITTERING
                        (
                        TIDSPUNKT,
                        BRUKERNOTIFIKASJON_BESTILLING_ID,
                        DOKNOTIFIKASJON_STATUS,
                        MELDING,
                        DISTRIBUSJON_ID,
                        JSON_PAYLOAD
                        )
                VALUES  ( CURRENT_TIMESTAMP, :brukernotifikasjon_bestilling_id, :doknotifikasjon_status, :melding, :distribusjon_id, :json_payload )
                """, parameterSource);
    }

    public List<Kvittering> hentKvitteringer(String bestillingsId) {
        SqlParameterSource parms = new MapSqlParameterSource()
                .addValue("bestillingsId", bestillingsId);

        String sql = """
                SELECT * FROM EKSTERN_VARSEL_KVITTERING
                WHERE BRUKERNOTIFIKASJON_BESTILLING_ID = :bestillingsId
                ORDER BY TIDSPUNKT
                """;
        return jdbc.query(sql, parms, rowMapper);

    }

    public int hentAntallUkvitterteVarslerForsoktSendt(long timerForsinkelse) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("date", new Date(Instant.now().minusSeconds(60 * 60 * timerForsinkelse).toEpochMilli()));

        // language=SQL
        String sql = """
             select count(*)
             from BRUKERNOTIFIKASJON
             where VARSEL_KVITTERING_STATUS = 'IKKE_SATT'
             and STATUS = 'SENDT'
             and FORSOKT_SENDT < :date
            """;

        return jdbc.queryForObject(sql, parameterSource, int.class);
    }
}
