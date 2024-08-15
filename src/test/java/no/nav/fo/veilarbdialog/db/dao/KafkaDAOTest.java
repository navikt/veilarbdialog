package no.nav.fo.veilarbdialog.db.dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaDAOTest extends BaseDAOTest {

    private static KafkaDAO kafkaDAO;

    @BeforeAll
    public static void setup() {
        kafkaDAO = new KafkaDAO(jdbc.getJdbcTemplate());
    }


    @Test
    void kan_sette_inn_og_slette_feilende_aktorid() {
        kafkaDAO.insertFeiletAktorId("123456789");

        List<String> alleFeilendeMeldinger = kafkaDAO.hentAlleFeilendeAktorId();
        String aktorId = alleFeilendeMeldinger.get(0);

        assertThat(aktorId).isEqualTo("123456789");
        kafkaDAO.slettFeiletAktorId("123456789");
        List<String> alleFeilendeAktorId = kafkaDAO.hentAlleFeilendeAktorId();

        assertThat(alleFeilendeAktorId).isEmpty();
    }

}
