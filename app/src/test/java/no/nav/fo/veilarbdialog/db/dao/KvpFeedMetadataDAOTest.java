package no.nav.fo.veilarbdialog.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Test;

import no.nav.fo.IntegrasjonsTest;

public class KvpFeedMetadataDAOTest extends IntegrasjonsTest {

    @Inject
    private KvpFeedMetadataDAO kvpFeedMetadataDAO;

    @Test
    public void siste_id_skal_være_samme_som_forrige_oppdaterte() {

        kvpFeedMetadataDAO.oppdaterSisteFeedId(5);
        assertThat(kvpFeedMetadataDAO.hentSisteId()).isEqualTo(5);
    }

}
