package no.nav.fo.veilarbdialog.db.dao;

import lombok.Data;
import no.nav.fo.veilarbdialog.domain.AktivitetId;
import no.nav.fo.veilarbdialog.domain.DatavarehusEvent;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class DataVarehusDAOTest extends BaseDAOTest {

    private static DataVarehusDAO dataVarehusDAO;

    @BeforeAll
    public static void setup() {
        dataVarehusDAO = new DataVarehusDAO(jdbc.getJdbcTemplate());
    }

    @Test
    void insertEvent() {

        DialogData dialog = DialogData.builder().id(1).aktivitetId(AktivitetId.of("aktivitet")).aktorId("aktor").build();
        String loggedInUser = "SYSTEM";
        dataVarehusDAO.insertEvent(dialog, DatavarehusEvent.VENTER_PAA_BRUKER, loggedInUser);

        DatavarehusData data = jdbc.getJdbcTemplate().queryForObject("select * from event", new BeanPropertyRowMapper<>(DatavarehusData.class));

        assertThat(data).isNotNull();
        assertThat(data.dialogId).isEqualTo(dialog.getId());
        assertThat(data.tidspunkt).isNotNull();
        assertThat(data.aktorId).isEqualTo(dialog.getAktorId());
        assertThat(data.aktivitetId).isEqualTo(dialog.getAktivitetId().getId());
        assertThat(data.event).isEqualTo(DatavarehusEvent.VENTER_PAA_BRUKER.toString());

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
