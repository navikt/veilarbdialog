package no.nav.fo.veilarbdialog.db.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static no.nav.fo.IntegationTest.uniktTidspunkt;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class FeedMetaDataDAOTest {

    @Autowired
    private FeedMetaDataDAO feedMetaDataDAO;

    @Test
    public void skal_kunne_sette_og_hente_sist_lest_tid() {
        Date uniktTidspunkt = uniktTidspunkt();
        feedMetaDataDAO.oppdaterSisteLest(uniktTidspunkt);
        ZonedDateTime lestTidspunkt = feedMetaDataDAO.hentSisteLestTidspunkt();
        assertThat(lestTidspunkt)
                .isEqualTo(ZonedDateTime.ofInstant(uniktTidspunkt.toInstant(), ZoneId.systemDefault()));
    }

}
