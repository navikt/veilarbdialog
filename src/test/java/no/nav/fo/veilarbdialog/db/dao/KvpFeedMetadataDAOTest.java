package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.IntegationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
public class KvpFeedMetadataDAOTest extends IntegationTest {

    @Autowired
    private KvpFeedMetadataDAO kvpFeedMetadataDAO;

    @Test
    public void siste_id_skal_vare_samme_som_forrige_oppdaterte() {
        kvpFeedMetadataDAO.oppdaterSisteFeedId(5);
        assertThat(kvpFeedMetadataDAO.hentSisteId()).isEqualTo(5);
    }

}
