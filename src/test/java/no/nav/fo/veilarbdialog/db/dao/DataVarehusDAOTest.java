package no.nav.fo.veilarbdialog.db.dao;

import lombok.Data;
import no.nav.fo.IntegationTest;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Date;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
class DataVarehusDAOTest extends IntegationTest {

    @Autowired
    private DataVarehusDAO dataVarehusDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void insertEvent() {

        DialogData dialog = DialogData.builder().id(1).aktivitetId("aktivitet").aktorId("aktor").build();
        dataVarehusDAO.insertEvent(dialog, DatavarehusEvent.VENTER_PAA_BRUKER);

        DatavarehusData data = jdbc.queryForObject("select * from event", DatavarehusData.class);

        assertThat(data).isNotNull();
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

    @Data
    private static class DatavarehusData {
        long dialogId;
        String eventId;
        Date tidspunkt;
        String aktorId;
        String aktivitetId;
        String event;
    }

}
