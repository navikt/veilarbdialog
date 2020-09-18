package no.nav.fo.veilarbdialog.db;

import no.nav.fo.IntegationTest;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
public class KafkaDAOTest extends IntegationTest {

    @Autowired
    private KafkaDAO kafkaDAO;

    @Test
    public void kan_inserte_feilende_aktorid() {
        kafkaDAO.insertFeiletAktorId("123456789");
        List<String> alleFeilendeMeldinger = kafkaDAO.hentAlleFeilendeAktorId();
        String aktorId = alleFeilendeMeldinger.get(0);

        assertThat(aktorId).isEqualTo("123456789");
    }

    @Test
    public void kan_slette_feilende_aktorid() {
        kafkaDAO.insertFeiletAktorId("123456789");

        kafkaDAO.slettFeiletAktorId("123456789");
        List<String> alleFeilendeAktorId = kafkaDAO.hentAlleFeilendeAktorId();

        assertThat(alleFeilendeAktorId.size()).isEqualTo(0);
    }

}
