package no.nav.fo.veilarbdialog.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Test;

import no.nav.fo.IntegrasjonsTest;

public class KvpFeedConsumerDAOTest extends IntegrasjonsTest {

    @Inject
    private KvpFeedConsumerDAO kvpFeedConsumerDAO;

    @Test
    public void siste_id_skal_være_samme_som_forrige_oppdaterte() {

        kvpFeedConsumerDAO.oppdaterSisteFeedId(5);
        assertThat(kvpFeedConsumerDAO.hentSisteId()).isEqualTo(5);
    }

}
