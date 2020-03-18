package no.nav.fo.veilarbdialog.db.dao;

import no.nav.common.auth.SubjectHandler;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;

import static no.nav.sbl.sql.DbConstants.CURRENT_TIMESTAMP;

@Component
public class DataVarehusDAO {

    static final String EVENT_ID = "event_id";
    static final String DIALOGID = "dialogid";
    static final String EVENT = "event";
    static final String TIDSPUNKT = "tidspunkt";
    static final String AKTOR_ID = "aktor_id";
    static final String AKTIVITET_ID = "aktivitet_id";
    static final String LAGT_INN_AV = "lagt_inn_av";
    static final String EVENT_TABELL = "EVENT";

    private final JdbcTemplate jdbc;

    @Inject
    public DataVarehusDAO(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public void insertEvent(DialogData dialogData, DatavarehusEvent datavarehusEvent) {
        Long nextSeq = SqlUtils.select(jdbc, "dual", DataVarehusDAO::hentLongFraForsteKollone)
                .column("EVENT_ID_SEQ.NEXTVAL")
                .execute();

        SqlUtils.insert(jdbc, EVENT_TABELL)
                .value(EVENT_ID, nextSeq)
                .value(DIALOGID, dialogData.getId())
                .value(EVENT, datavarehusEvent.toString())
                .value(TIDSPUNKT, CURRENT_TIMESTAMP)
                .value(AKTOR_ID, dialogData.getAktorId())
                .value(AKTIVITET_ID, dialogData.getAktivitetId())
                .value(LAGT_INN_AV, getLagtInnAv())
                .execute();
    }

    private static Long hentLongFraForsteKollone(ResultSet resultSet) throws SQLException {
        return resultSet.getLong(1);
    }

    private String getLagtInnAv() {
        return SubjectHandler.getIdent().orElse("SYSTEM");
    }

}
