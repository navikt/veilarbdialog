package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.util.DatabaseUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collections;
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

    public EskaleringsvarselEntity opprett(long tilhorendeDialogId, String aktorId, String opprettetAv, String opprettetBegrunnelse) {
        ZonedDateTime opprettetDato = ZonedDateTime.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", aktorId)
                .addValue("opprettetAv", opprettetAv)
                .addValue("opprettetDato", opprettetDato)
                .addValue("dialogId", tilhorendeDialogId)
                .addValue("begrunnelse", opprettetBegrunnelse);

        // TODO skal vi ha med brukernotifikasjons eventid?

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        String sql = """
                INSERT INTO ESKALERINGSVARSEL (
                    AKTOR_ID,
                    OPPRETTET_AV,
                    OPPRETTET_DATO,
                    TILHORENDE_DIALOG_ID,
                    OPPRETTET_BEGRUNNELSE
                    )
                VALUES (
                    :aktorId,
                    :opprettetAv,
                    :opprettetDato,
                    :dialogId,
                    :begrunnelse)
                """;

        jdbc.update(sql, params, keyHolder);
        long key = keyHolder.getKey().longValue();
        return new EskaleringsvarselEntity(
                key,
                tilhorendeDialogId,
                0,
                aktorId,
                opprettetAv,
                opprettetDato,
                opprettetBegrunnelse,
                null,
                null,
                null
                );
    }

    public void stopp(long varselId, String avsluttetAv, String avsluttetBegrunnelse) {
        throw new NotImplementedException();
    }

    public Optional<EskaleringsvarselEntity> hentGjeldende(AktorId aktorId) {
        String sql = """
                SELECT * FROM ESKALERINGSVARSEL WHERE AKTOR_ID = :aktor_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktor_id", aktorId.get());
        try {
            return Optional.of(jdbc.queryForObject(sql, params, rowMapper));
        } catch (EmptyResultDataAccessException emptyResultDataAccessException) {
            return Optional.empty();
        }
    }

    public List<EskaleringsvarselEntity> hentHistorikk(AktorId aktorId) {
        return Collections.emptyList();
    }

}
