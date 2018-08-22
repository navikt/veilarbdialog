package no.nav.fo.veilarbdialog.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.BeforeClass;
import org.junit.Test;

import no.nav.fo.veilarbdialog.db.DbTest;

public class KvpFeedMetadataDAOTest extends DbTest {

    @Inject
    private KvpFeedMetadataDAO kvpFeedMetadataDAO;

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(Arrays.asList(KvpFeedMetadataDAO.class));
    }

    @Test
    public void siste_id_skal_være_samme_som_forrige_oppdaterte() {

        kvpFeedMetadataDAO.oppdaterSisteFeedId(5);
        assertThat(kvpFeedMetadataDAO.hentSisteId()).isEqualTo(5);
    }

}
