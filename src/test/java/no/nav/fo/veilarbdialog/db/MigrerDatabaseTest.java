package no.nav.fo.veilarbdialog.db;

import no.nav.fo.IntegationTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.util.Collections;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class MigrerDatabaseTest extends IntegationTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @BeforeClass
    public static void initContext() {
        initSpringContext(Collections.emptyList());
    }

    @Test
    public void kanQueryeDatabasen() {
        assertThat(jdbcTemplate.queryForList("SELECT * FROM DIALOG"), empty());
    }

}
