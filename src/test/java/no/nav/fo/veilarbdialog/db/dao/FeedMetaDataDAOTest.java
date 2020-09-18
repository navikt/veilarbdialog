package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.IntegationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FeedMetaDataDAOTest extends IntegationTest {

    @Autowired
    private FeedMetaDataDAO feedMetaDataDAO;

    @Test
    public void skal_kunne_sette_og_hente_sist_lest_tid() {
        Date uniktTidspunkt = uniktTidspunkt();
        feedMetaDataDAO.oppdaterSisteLest(uniktTidspunkt);
        Date lestTidspunkt = feedMetaDataDAO.hentSisteLestTidspunkt();
        assertThat(lestTidspunkt).isEqualTo(uniktTidspunkt);
    }

}
