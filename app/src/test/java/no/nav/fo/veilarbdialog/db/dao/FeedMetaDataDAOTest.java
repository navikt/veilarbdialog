package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import no.nav.fo.IntegrasjonsTest;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;

public class FeedMetaDataDAOTest extends IntegrasjonsTest {

    @Inject
    private FeedMetaDataDAO feedMetaDataDAO;

    @Test
    public void skal_kunne_sette_og_hente_sist_lest_tid() {
        Date uniktTidspunkt = uniktTidspunkt();
        feedMetaDataDAO.oppdaterSisteLest(uniktTidspunkt);
        Date lestTidspunkt = feedMetaDataDAO.hentSisteLestTidspunkt();
        assertThat(lestTidspunkt).isEqualTo(uniktTidspunkt);
    }

    @SneakyThrows
    private Date uniktTidspunkt() {
        sleep(1);
        Date date = new Date();
        sleep(1);
        return date;
    }
}
