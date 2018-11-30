package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.IntegationTest;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class KvpFeedMetadataDAOTest extends IntegationTest {

    @Inject
    private KvpFeedMetadataDAO kvpFeedMetadataDAO;

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(Arrays.asList(KvpFeedMetadataDAO.class));
    }

    @Test
    public void siste_id_skal_vare_samme_som_forrige_oppdaterte() {
        kvpFeedMetadataDAO.oppdaterSisteFeedId(5);
        assertThat(kvpFeedMetadataDAO.hentSisteId()).isEqualTo(5);
    }

}
