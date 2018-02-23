package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.SQLException;

import static no.nav.sbl.sql.DbConstants.CURRENT_TIMESTAMP;

@Component
public class DataVarehusDAO {

    protected static final String EVENT_ID = "event_id";
    protected static final String DIALOGID = "dialogid";
    protected static final String EVENT = "event";
    protected static final String TIDSPUNKT = "tidspunkt";
    protected static final String AKOR_ID = "akor_id";
    protected static final String AKTIVITET_ID = "aktivitet_id";
    protected static final String EVENT_TABELL = "EVENT";

    private final JdbcTemplate jdbc;
    private final DataSource ds;

    @Inject
    public DataVarehusDAO(DataSource ds, JdbcTemplate jdbcTemplate) {
        this.ds = ds;
        this.jdbc = jdbcTemplate;
    }

    public void insertEvent(DialogData dialogData, DatavarehusEvent datavarehusEvent) {
        Long nextSeq = SqlUtils.select(ds, "dual", DataVarehusDAO::sqlFunction)
                .column("EVENT_ID_SEQ.NEXTVAL")
                .execute();

        SqlUtils.insert(jdbc, EVENT_TABELL)
                .value(EVENT_ID, nextSeq)
                .value(DIALOGID, dialogData.getId())
                .value(EVENT, datavarehusEvent.toString())
                .value(TIDSPUNKT, CURRENT_TIMESTAMP)
                .value(AKOR_ID, dialogData.getAktorId())
                .value(AKTIVITET_ID, dialogData.getAktivitetId())
                .execute();
    }

    private static Long sqlFunction(ResultSet resultSet) throws SQLException {
        return resultSet.getLong(1);
    }
}
