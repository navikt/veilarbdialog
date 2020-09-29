package no.nav.fo.veilarbdialog.db.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class KvpFeedMetadataDAOTest {

    @Autowired
    private KvpFeedMetadataDAO kvpFeedMetadataDAO;

    @Test
    public void siste_id_skal_vare_samme_som_forrige_oppdaterte() {
        kvpFeedMetadataDAO.oppdaterSisteFeedId(5);
        assertThat(kvpFeedMetadataDAO.hentSisteId()).isEqualTo(5);
    }

}
