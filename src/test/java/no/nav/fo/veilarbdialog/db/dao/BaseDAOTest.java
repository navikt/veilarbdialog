package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class BaseDAOTest {

    static JdbcTemplate jdbc = new JdbcTemplate(LocalDatabaseSingleton.INSTANCE.getPostgres());
}
