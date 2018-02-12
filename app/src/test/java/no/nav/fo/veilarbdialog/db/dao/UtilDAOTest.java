package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.IntegrasjonsTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

class UtilDAOTest extends IntegrasjonsTest {

    @Inject
    private UtilDAO utilDAO;

    @Test
    void getTimestampFromDB() {
        assertThat(utilDAO.getTimestampFromDB()).isNotNull();
    }
}