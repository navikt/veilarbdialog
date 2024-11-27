package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;
import no.nav.fo.veilarbdialog.util.DatabaseUtils;
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbDialogSqlParameterSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EskaleringsvarselRepository {

    private final NamedParameterJdbcTemplate jdbc;

    RowMapper<EskaleringsvarselEntity> rowMapper = (rs, rowNum) -> new EskaleringsvarselEntity(
            rs.getLong("id"),
            rs.getLong("tilhorende_dialog_id"),
            rs.getLong("tilhorende_brukernotifikasjon_id"),
            Optional.ofNullable(DatabaseUtils.hentMaybeUUID( rs,"tilhorende_minside_varsel"))
                    .map(MinSideVarselId::new)
                    .orElse(null),
            rs.getString("aktor_id"),
            rs.getString("opprettet_av"),
            DatabaseUtils.hentZonedDateTime(rs, "opprettet_dato"),
            rs.getString("opprettet_begrunnelse"),
            DatabaseUtils.hentZonedDateTime(rs, "avsluttet_dato"),
            rs.getString("avsluttet_av"),
            rs.getString("avsluttet_begrunnelse"),
            rs.getBoolean("sendt_til_oversikten")
    );

    public EskaleringsvarselEntity opprett(long tilhorendeDialogId, MinSideVarselId varselId, String aktorId, String opprettetAv, String opprettetBegrunnelse) {
        ZonedDateTime opprettetDato = ZonedDateTime.now();
        var params = new VeilarbDialogSqlParameterSource()
                .addValue("aktorId", aktorId)
                .addValue("opprettetAv", opprettetAv)
                .addValue("opprettetDato", opprettetDato)
                .addValue("dialogId", tilhorendeDialogId)
                .addValue("begrunnelse", opprettetBegrunnelse)
                .addValue("varselId", varselId.getValue());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        Long key;
        String sql = """
          INSERT INTO ESKALERINGSVARSEL (
                    AKTOR_ID, GJELDENDE, OPPRETTET_AV, OPPRETTET_DATO, TILHORENDE_DIALOG_ID, tilhorende_minside_varsel, OPPRETTET_BEGRUNNELSE)
          VALUES ( :aktorId, :aktorId,   :opprettetAv, :opprettetDato, :dialogId,            :varselId,           :begrunnelse)
                """;
        try {
            jdbc.update(sql, params, keyHolder, new String[]{"id"});

            Number generatedKey = keyHolder.getKey();
            if (generatedKey == null) {
                throw new DataAccessResourceFailureException("Generated key not present");
            }
            key = generatedKey.longValue();
        } catch (DuplicateKeyException dke) {
            throw new AktivEskaleringException("Pågående start-eksalering.");
        }

        return new EskaleringsvarselEntity(
                key,
                tilhorendeDialogId,
                0,
                varselId,
                aktorId,
                opprettetAv,
                opprettetDato,
                opprettetBegrunnelse,
                null,
                null,
                null,
                false
                );
    }

    public void stop(long varselId, String begrunnelse, NavIdent avsluttetAv) {
        var params = new VeilarbDialogSqlParameterSource()
                .addValue("avsluttetDato", ZonedDateTime.now())
                .addValue("avsluttetAv", avsluttetAv.get())
                .addValue("avsluttetBegrunnelse", begrunnelse)
                .addValue("varselId", varselId);
        String sql = """
                UPDATE ESKALERINGSVARSEL SET
                    AVSLUTTET_DATO = :avsluttetDato,
                    AVSLUTTET_AV = :avsluttetAv,
                    AVSLUTTET_BEGRUNNELSE = :avsluttetBegrunnelse,
                    GJELDENDE = null
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

    public boolean stopPeriode(UUID oppfolgingsperiodeUuid) {
        String sql = """
                UPDATE ESKALERINGSVARSEL
                set avsluttet_av = 'SYSTEM', 
                    avsluttet_dato = current_timestamp , 
                    avsluttet_begrunnelse = 'Oppfolging avsluttet',
                    gjeldende = NULL
                WHERE avsluttet_dato is null 
                    and exists(select * from dialog 
                               where tilhorende_dialog_id = dialog_id 
                               and oppfolgingsperiode_uuid = :oppfolgingsperiode)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("oppfolgingsperiode", oppfolgingsperiodeUuid.toString());
        int rowsUpdated = jdbc.update(sql, params);
        return rowsUpdated != 0;
    }

    public Optional<EskaleringsvarselEntity> hentVarsel(long varselId) {
        String sql = """
                SELECT * FROM ESKALERINGSVARSEL WHERE ID = :varselId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("varselId", varselId);
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, rowMapper));
        } catch (EmptyResultDataAccessException emptyResultDataAccessException) {
            return Optional.empty();
        }
    }

    public List<EskaleringsvarselEntity> hentUsendteGjeldendeVarslerEldreEnn(LocalDateTime tidspunkt) {
        String sql = """
                SELECT * FROM ESKALERINGSVARSEL
                WHERE opprettet_dato < :tidspunkt
                AND gjeldende IS NOT NULL
                AND sendt_til_oversikten IS FALSE
                """;
        var params = new MapSqlParameterSource()
                .addValue("tidspunkt", tidspunkt);
        return jdbc.query(sql, params, rowMapper);
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

    public void markerVarselSomSendt(AktorId aktorId, LocalDateTime tidspunkt) {
        String sql = """
                UPDATE ESKALERINGSVARSEL 
                SET sendt_til_oversikten = true
                WHERE opprettet_dato < :tidspunkt
                AND gjeldende IS NOT NULL
                AND sendt_til_oversikten IS FALSE
                AND AKTOR_ID = :aktor_id
                """;
        var params = new MapSqlParameterSource()
                .addValue("aktor_id", aktorId)
                .addValue("tidspunkt", tidspunkt);

        jdbc.query(sql, params, rowMapper);
    }
}
