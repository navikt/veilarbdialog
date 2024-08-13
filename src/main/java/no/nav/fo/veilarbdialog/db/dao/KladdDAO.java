package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.Kladd;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLType;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KladdDAO {

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional
    public void upsertKladd(Kladd kladd) {

        Long id = Optional.ofNullable(kladd.getDialogId()).map(Long::parseLong).orElse(null);
        long kladdSeq = Optional
                .ofNullable(jdbc.getJdbcTemplate().queryForObject("select nextval('KLADD_ID_SEQ')", Long.class))
                .orElseThrow(IllegalStateException::new);

        jdbc.update("insert into KLADD (AKTOR_ID, DIALOG_ID, AKTIVITET_ID, OVERSKRIFT, TEKST, LAGT_INN_AV, OPPDATERT, UNIQUE_SEQ) " +
                        "values (:aktorId, :id, :aktivitetId, :overskrift, :tekst, :lagtInnAv, :oppdatert, :kladdSeq)",
                new MapSqlParameterSource("aktorId", kladd.getAktorId())
                        .addValue("id", id)
                        .addValue("aktivitetId", kladd.getAktivitetId())
                        .addValue("overskrift", kladd.getOverskrift())
                        .addValue("tekst", kladd.getTekst())
                        .addValue("lagtInnAv", kladd.getLagtInnAv())
                        .addValue("oppdatert", Timestamp.valueOf(LocalDateTime.now()))
                        .addValue("kladdSeq", kladdSeq)
        );

        //henvendelser på eksisterende tråer har ikke aktivitetId
        jdbc.update("""
                        \
                         delete from KLADD \
                         where UNIQUE_SEQ < :kladdSeq \
                         and LAGT_INN_AV = :lagtInnAv\
                         and AKTOR_ID = :aktorId \
                         and (DIALOG_ID = :dialogId or (DIALOG_ID is null and :dialogId is null))\
                         and (DIALOG_ID is not null or (AKTIVITET_ID = :aktivitetId or (AKTIVITET_ID is null and :aktivitetId is null))) \s""",
                new MapSqlParameterSource("kladdSeq", kladdSeq)
                        .addValue("lagtInnAv", kladd.getLagtInnAv())
                        .addValue("aktorId", kladd.getAktorId())
                        .addValue("dialogId", kladd.getDialogId(), Types.BIGINT)
                        .addValue("aktivitetId", kladd.getAktivitetId())
        );

    }

    public List<Kladd> getKladder(String aktorId, String lagtInnAv) {
        return jdbc.query("""
                    select * from KLADD where 
                    AKTOR_ID = :aktorId and 
                    LAGT_INN_AV = :lagtInnAv 
                    order by UNIQUE_SEQ
                """.trim(),
                new MapSqlParameterSource("aktorId", aktorId)
                    .addValue("lagtInnAv", lagtInnAv),
                (rs, rowNum) -> Kladd.builder()
                        .aktorId(rs.getString("AKTOR_ID"))
                        .dialogId(rs.getString("DIALOG_ID"))
                        .aktivitetId(rs.getString("AKTIVITET_ID"))
                        .overskrift(rs.getString("OVERSKRIFT"))
                        .tekst(rs.getString("TEKST"))
                        .lagtInnAv(rs.getString("LAGT_INN_AV"))
                        .build()

        );
    }

    public void slettKladderGamlereEnnTimer(long timer) {
        LocalDateTime olderThanThis = LocalDateTime.now().minusHours(timer);
        jdbc.update("delete from KLADD where OPPDATERT <= :olderThanThis",
                new MapSqlParameterSource("olderThanThis", olderThanThis));
    }

    public void slettKladd(Kladd kladd) {
        //henvendelser på eksisterende tråer har ikke aktivitetId
        jdbc.update("""
                        delete from KLADD \
                        where AKTOR_ID = :aktorId \
                        and LAGT_INN_AV = :lagtInnAv \
                        and (DIALOG_ID = :dialogId or (DIALOG_ID is null and :dialogId is null)) \
                        and (DIALOG_ID is not null or (AKTIVITET_ID = :aktivitetId or (AKTIVITET_ID is null and :aktivitetId is null)))""",
                new MapSqlParameterSource("aktorId", kladd.getAktorId())
                        .addValue("lagtInnAv", kladd.getLagtInnAv())
                        .addValue("dialogId", kladd.getDialogId(), Types.BIGINT)
                        .addValue("aktivitetId", kladd.getAktivitetId(), Types.VARCHAR)
        );
    }

}
