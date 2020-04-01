package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.Kladd;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.order.OrderClause;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class KladdDAO {

    private static final String AKTOR_ID = "AKTOR_ID";
    private static final String DIALOG_ID = "DIALOG_ID";
    private static final String AKTIVITET_ID = "AKTIVITET_ID";
    private static final String OVERSKRIFT = "OVERSKRIFT";
    private static final String TEKST = "TEKST";
    private static final String LAGT_INN_AV = "LAGT_INN_AV";
    private static final String OPPDATERT = "OPPDATERT";
    private static final String UNIQUE_SEQ = "UNIQUE_SEQ";

    private static final String KLADD_TABELL = "KLADD";

    private final JdbcTemplate jdbc;

    @Inject
    public KladdDAO(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }


    private WhereClause whereKladdEq(Kladd kladd){
        WhereClause eqAktorId = WhereClause.equals(AKTOR_ID, kladd.aktorId);
        WhereClause eqLagtInnAv = WhereClause.equals(LAGT_INN_AV, kladd.lagtInnAv);

        WhereClause eqDialogId = Optional.ofNullable(kladd.dialogId)
                .map(id -> WhereClause.equals(DIALOG_ID, Long.parseLong(id)))
                .orElse(WhereClause.isNull(DIALOG_ID));
        WhereClause eqAktivitetId = Optional.ofNullable(kladd.aktivitetId)
                .map(aktivitetId -> WhereClause.equals(AKTIVITET_ID, kladd.aktivitetId))
                .orElse(WhereClause.isNull(AKTIVITET_ID));


        return eqAktorId.and(eqDialogId)
                .and(eqAktivitetId)
                .and(eqAktorId)
                .and(eqLagtInnAv);
    }

    @Transactional
    public void upsertKladd(Kladd kladd) {
        Long id = Optional.ofNullable(kladd.dialogId).map(Long::parseLong).orElse(null);
        Long kladdSeq = SqlUtils.nextFromSeq(jdbc,"KLADD_ID_SEQ").execute();

        SqlUtils.insert(jdbc, KLADD_TABELL)
                .value(AKTOR_ID, kladd.aktorId)
                .value(DIALOG_ID, id)
                .value(AKTIVITET_ID, kladd.aktivitetId)
                .value(OVERSKRIFT, kladd.overskrift)
                .value(TEKST, kladd.tekst)
                .value(LAGT_INN_AV, kladd.lagtInnAv)
                .value(OPPDATERT, Timestamp.valueOf(LocalDateTime.now()))
                .value(UNIQUE_SEQ, kladdSeq)
                .execute();

        SqlUtils.delete(jdbc, KLADD_TABELL)
                .where(whereKladdEq(kladd).and(WhereClause.lt(UNIQUE_SEQ, kladdSeq)))
                .execute();

    }

    public List<Kladd> getKladder(String aktorId, String lagtInnAv) {
        WhereClause eqAktorId = WhereClause.equals(AKTOR_ID, aktorId);
        WhereClause eqLagtInnAv = WhereClause.equals(LAGT_INN_AV, lagtInnAv);

        return SqlUtils.select(jdbc, KLADD_TABELL, KladdDAO::toStatusnumbers)
                .column("*")
                .where(eqAktorId.and(eqLagtInnAv))
                .orderBy(OrderClause.desc(UNIQUE_SEQ))
                .executeToList();
    }

    public void slettKladderGamlereEnnTimer(long timer){
        LocalDateTime olderThenThis = LocalDateTime.now().minusHours(timer);
        WhereClause whereOlderOrEqualThen = WhereClause.lteq(OPPDATERT, Timestamp.valueOf(olderThenThis));

        SqlUtils.delete(jdbc, KLADD_TABELL)
                .where(whereOlderOrEqualThen)
                .execute();
    }

    public void slettKladd(Kladd kladd){
        SqlUtils.delete(jdbc, KLADD_TABELL)
                .where(whereKladdEq(kladd))
                .execute();
    }

    private static Kladd toStatusnumbers(ResultSet rs) throws SQLException {
        return Kladd.builder()
                .aktorId(rs.getString(AKTOR_ID))
                .dialogId(rs.getString(DIALOG_ID))
                .aktivitetId(rs.getString(AKTIVITET_ID))
                .overskrift(rs.getString(OVERSKRIFT))
                .tekst(rs.getString(TEKST))
                .lagtInnAv(rs.getString(LAGT_INN_AV))
                .build();
    }
}
