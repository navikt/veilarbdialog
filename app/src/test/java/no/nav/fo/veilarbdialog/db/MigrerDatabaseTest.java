package no.nav.fo.veilarbdialog.db;

import no.nav.fo.IntegrasjonsTest;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;

import java.util.Map;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class MigrerDatabaseTest extends IntegrasjonsTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void kanQueryeDatabasen() {
        assertThat(jdbcTemplate.queryForList("SELECT * FROM DIALOG"), empty());
    }

}