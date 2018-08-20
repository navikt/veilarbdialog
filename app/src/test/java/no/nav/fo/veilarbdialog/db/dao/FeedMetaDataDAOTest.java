package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.db.DbTest;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedMetaDataDAOTest extends DbTest {

    @Inject
    private FeedMetaDataDAO feedMetaDataDAO;

    @BeforeClass
    public static void addSpringBeans() {
        annotationConfigApplicationContext.registerBean(FeedMetaDataDAO.class);
    }

    @Test
    public void skal_kunne_sette_og_hente_sist_lest_tid() {
        Date uniktTidspunkt = uniktTidspunkt();
        feedMetaDataDAO.oppdaterSisteLest(uniktTidspunkt);
        Date lestTidspunkt = feedMetaDataDAO.hentSisteLestTidspunkt();
        assertThat(lestTidspunkt).isEqualTo(uniktTidspunkt);
    }

}
