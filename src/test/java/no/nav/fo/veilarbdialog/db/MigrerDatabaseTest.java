package no.nav.fo.veilarbdialog.db;

import no.nav.fo.IntegationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
public class MigrerDatabaseTest extends IntegationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void kanQueryeDatabasen() {
        assertThat(jdbcTemplate.queryForList("SELECT * FROM DIALOG")).isEmpty();
    }

}
