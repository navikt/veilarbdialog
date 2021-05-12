package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.Kladd;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KladdDAO {

    private final JdbcTemplate jdbc;

    @Transactional
    public void upsertKladd(Kladd kladd) {

        Long id = Optional.ofNullable(kladd.getDialogId()).map(Long::parseLong).orElse(null);
        long kladdSeq = Optional
                .ofNullable(jdbc.queryForObject("select KLADD_ID_SEQ.nextval from DUAL", Long.class))
                .orElseThrow(IllegalStateException::new);

        jdbc.update("insert into KLADD (AKTOR_ID, DIALOG_ID, AKTIVITET_ID, OVERSKRIFT, TEKST, LAGT_INN_AV, OPPDATERT, UNIQUE_SEQ) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?)",
                kladd.getAktorId(),
                id,
                kladd.getAktivitetId(),
                kladd.getOverskrift(),
                kladd.getTekst(),
                kladd.getLagtInnAv(),
                Timestamp.valueOf(LocalDateTime.now()),
                kladdSeq);


        jdbc.update("" +
                        " delete from KLADD " +
                        " where UNIQUE_SEQ < ? " +
                        " and LAGT_INN_AV = ?" +
                        " and AKTOR_ID = ? " +
                        " and (DIALOG_ID = ? or (DIALOG_ID is null and ? is null))" +
                        //henvendelser på eksisterende tråer har ikke aktivitetId
                        " and (DIALOG_ID is not null or (AKTIVITET_ID = ? or (AKTIVITET_ID is null and ? is null)))  ",
                kladdSeq,
                kladd.getLagtInnAv(),
                kladd.getAktorId(),
                kladd.getDialogId(),
                kladd.getDialogId(),
                kladd.getAktivitetId(),
                kladd.getAktivitetId()
        );

    }

    public List<Kladd> getKladder(String aktorId, String lagtInnAv) {
        return jdbc.query("select * from KLADD where " +
                        "AKTOR_ID = ? and " +
                        "LAGT_INN_AV = ? " +
                        "order by UNIQUE_SEQ",
                (rs, rowNum) -> Kladd.builder()
                        .aktorId(rs.getString("AKTOR_ID"))
                        .dialogId(rs.getString("DIALOG_ID"))
                        .aktivitetId(rs.getString("AKTIVITET_ID"))
                        .overskrift(rs.getString("OVERSKRIFT"))
                        .tekst(rs.getString("TEKST"))
                        .lagtInnAv(rs.getString("LAGT_INN_AV"))
                        .build(),
                aktorId,
                lagtInnAv
        );
    }

    public void slettKladderGamlereEnnTimer(long timer) {
        LocalDateTime olderThanThis = LocalDateTime.now().minusHours(timer);
        jdbc.update("delete from KLADD where " +
                        "OPPDATERT <= ?",
                olderThanThis);
    }

    public void slettKladd(Kladd kladd) {

        jdbc.update("delete from KLADD " +
                        "where AKTOR_ID = ? " +
                        "and LAGT_INN_AV = ? " +
                        "and (DIALOG_ID = ? or (DIALOG_ID is null and ? is null)) " +
                        //henvendelser på eksisterende tråer har ikke aktivitetId
                        "and (DIALOG_ID is not null or (AKTIVITET_ID = ? or (AKTIVITET_ID is null and ? is null)))",
                kladd.getAktorId(),
                kladd.getLagtInnAv(),
                kladd.getDialogId(), kladd.getDialogId(),
                kladd.getAktivitetId(), kladd.getAktivitetId());
    }

}
