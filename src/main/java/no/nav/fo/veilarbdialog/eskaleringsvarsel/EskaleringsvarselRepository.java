package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EskaleringsvarselRepository {

    private final NamedParameterJdbcTemplate jdbc;

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

        int update = jdbc.update(sql, params, keyHolder);
        long key = keyHolder.getKey().longValue();
        return new EskaleringsvarselEntity(
                key,
                tilhorendeDialogId,
                aktorId,
                opprettetAv,
                opprettetDato,
                opprettetBegrunnelse,
                null,
                null,
                null,
                null
                );
    }

    public void stopp(long varselId, String avsluttetAv, String avsluttetBegrunnelse) {

    }

    public EskaleringsvarselEntity hentGjeldende(AktorId aktorId) {
        return null;
    }

    public List<EskaleringsvarselEntity> hentHistorikk(AktorId aktorId) {
        return Collections.emptyList();
    }

}
