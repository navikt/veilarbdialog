package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public abstract class BaseDAOTest {
    static NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(LocalDatabaseSingleton.INSTANCE.getPostgres());
}
