package no.nav.fo.veilarbdialog.db;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import no.nav.fo.IntegationTest;

import javax.inject.Inject;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.util.Collections;

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
