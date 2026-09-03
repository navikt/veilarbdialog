package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Provides the shared embedded Postgres database for {@code @DataJdbcTest} slices.
 * Schema migration is performed once by {@link LocalDatabaseSingleton}.
 */
@TestConfiguration
public class JdbcTestConfig {

    @Bean
    public DataSource dataSource() {
        return LocalDatabaseSingleton.INSTANCE.getPostgres();
    }
}
