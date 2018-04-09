package no.nav.fo.veilarbdialog.db.dao;

import lombok.*;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.sbl.sql.SqlUtils;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO.*;
import static org.assertj.core.api.Assertions.assertThat;

class DataVarehusDAOTest extends IntegrasjonsTest{

    @Inject
    private DataVarehusDAO dataVarehusDAO;

    @Inject
    private JdbcTemplate jdbc;


    @Test
    void insertEvent() {
        DialogData dialog = DialogData.builder().id(1).aktivitetId("aktivitet").aktorId("aktor").build();
        dataVarehusDAO.insertEvent(dialog, DatavarehusEvent.VENTER_PAA_BRUKER);

        DatavarehusData data = SqlUtils.select(jdbc, EVENT_TABELL, this::map)
                .column(DIALOGID)
                .column(EVENT_ID)
                .column(TIDSPUNKT)
                .column(AKTOR_ID)
                .column(AKTIVITET_ID)
                .column(EVENT)
                .execute();

        assertThat(data.dialogId).isEqualTo(dialog.getId());
        assertThat(data.tidspunkt).isNotNull();
        assertThat(data.aktorId).isEqualTo(dialog.getAktorId());
        assertThat(data.aktivitetId).isEqualTo(dialog.getAktivitetId());
        assertThat(data.event).isEqualTo(DatavarehusEvent.VENTER_PAA_BRUKER.toString());

    }

    @Test
    void skal_kunne_sette_inn_alle_event_typene() {
        Arrays.stream(DatavarehusEvent.values()).forEach(event -> dataVarehusDAO.insertEvent(nyDialog(), event));
    }

    private DatavarehusData map(ResultSet rs) throws SQLException {
        DatavarehusData datavarehusData = new DatavarehusData();
        datavarehusData.dialogId = rs.getLong(DIALOGID);
        datavarehusData.eventId = rs.getString(EVENT_ID);
        datavarehusData.tidspunkt = rs.getDate(TIDSPUNKT);
        datavarehusData.aktorId = rs.getString(AKTOR_ID);
        datavarehusData.aktivitetId = rs.getString(AKTIVITET_ID);
        datavarehusData.event = rs.getString(EVENT);

        return datavarehusData;
    }

    @Data
    static class DatavarehusData {
        long dialogId;
        String eventId;
        Date tidspunkt;
        String aktorId;
        String aktivitetId;
        String event;
    }

}