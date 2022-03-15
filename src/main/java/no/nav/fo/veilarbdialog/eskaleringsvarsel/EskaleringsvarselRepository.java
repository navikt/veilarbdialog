package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.util.DatabaseUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EskaleringsvarselRepository {

    private final NamedParameterJdbcTemplate jdbc;

    RowMapper<EskaleringsvarselEntity> rowMapper = (rs, rowNum) -> new EskaleringsvarselEntity(
            rs.getLong("id"),
            rs.getLong("tilhorende_dialog_id"),
            rs.getLong("tilhorende_brukernotifikasjon_id"),
            rs.getString("aktor_id"),
            rs.getString("opprettet_av"),
            DatabaseUtils.hentZonedDateTime(rs, "opprettet_dato"),
            rs.getString("opprettet_begrunnelse"),
            DatabaseUtils.hentZonedDateTime(rs, "avsluttet_dato"),
            rs.getString("avsluttet_av"),
            rs.getString("avsluttet_begrunnelse")
    );

    public EskaleringsvarselEntity opprett(long tilhorendeDialogId, long tilhorendeBrukernotifikasjonsId, String aktorId, String opprettetAv, String opprettetBegrunnelse) {
        ZonedDateTime opprettetDato = ZonedDateTime.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", aktorId)
                .addValue("opprettetAv", opprettetAv)
                .addValue("opprettetDato", opprettetDato)
                .addValue("dialogId", tilhorendeDialogId)
                .addValue("begrunnelse", opprettetBegrunnelse)
                .addValue("brukernotifikasjonsId", tilhorendeBrukernotifikasjonsId);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        String sql = """
                INSERT INTO ESKALERINGSVARSEL (
                    AKTOR_ID,
                    OPPRETTET_AV,
                    OPPRETTET_DATO,
                    TILHORENDE_DIALOG_ID,
                    TILHORENDE_BRUKERNOTIFIKASJON_ID,
                    OPPRETTET_BEGRUNNELSE
                    )
                VALUES (
                    :aktorId,
                    :opprettetAv,
                    :opprettetDato,
                    :dialogId,
                    :brukernotifikasjonsId,
                    :begrunnelse)
                """;

        jdbc.update(sql, params, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new DataAccessResourceFailureException("Generated key not present");
        }
        long key = generatedKey.longValue();
        return new EskaleringsvarselEntity(
                key,
                tilhorendeDialogId,
                tilhorendeBrukernotifikasjonsId,
                aktorId,
                opprettetAv,
                opprettetDato,
                opprettetBegrunnelse,
                null,
                null,
                null
                );
    }

    public void stop(long varselId, String begrunnelse, NavIdent avsluttetAv) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("avsluttetDato", Instant.now())
                .addValue("avsluttetAv", avsluttetAv.get())
                .addValue("avsluttetBegrunnelse", begrunnelse)
                .addValue("varselId", varselId);
        String sql = """
                UPDATE ESKALERINGSVARSEL SET AVSLUTTET_DATO = :avsluttetDato, AVSLUTTET_AV = :avsluttetAv, AVSLUTTET_BEGRUNNELSE = :avsluttetBegrunnelse
                WHERE ID = :varselId
                """;
        int update = jdbc.update(sql, params);
        assert update == 1;
    }

    public Optional<EskaleringsvarselEntity> hentGjeldende(AktorId aktorId) {
        String sql = """
                SELECT * FROM ESKALERINGSVARSEL WHERE AKTOR_ID = :aktor_id AND AVSLUTTET_DATO IS NULL
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktor_id", aktorId.get());
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, rowMapper));
        } catch (EmptyResultDataAccessException emptyResultDataAccessException) {
            return Optional.empty();
        }
    }

    public List<EskaleringsvarselEntity> hentHistorikk(AktorId aktorId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktor_id", aktorId.get());
        String sql = """
                SELECT * FROM ESKALERINGSVARSEL WHERE AKTOR_ID = :aktor_id
                ORDER BY OPPRETTET_DATO DESC
                """;
        return  jdbc.query(sql, params, rowMapper);
    }

}
