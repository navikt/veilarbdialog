package no.nav.fo.veilarbdialog.db;

import no.nav.fo.IntegationTest;
import no.nav.fo.veilarbdialog.db.dao.KafkaDAO;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class KafkaDAOTest extends IntegationTest {

    @Inject
    private KafkaDAO kafkaDAO;

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(Arrays.asList(KafkaDAO.class));
    }

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
