package no.nav.fo;

import no.nav.apiapp.util.StringUtils;
import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.sbl.jdbc.Database;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.*;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    static {
        setProperty(VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME, inMemoryUrl());
        setProperty(VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME, "sa");
        setProperty(VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME, "");
    }

    private static int counter;

    @Bean
    public DataSource dataSource() {
        return buildDataSource();
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public Database database(DataSource ds) {
        return new Database(new JdbcTemplate(ds));
    }

    public static DataSource buildDataSource() {
        String url = StringUtils.of(System.getProperty("database.url")).orElse(inMemoryUrl());
        return doBuildSingleConnectionDataSource(new DbCredentials()
                        .setUrl(url)
                        .setUsername("sa")
                        .setPassword(""),
                true
        );
    }

    private static String inMemoryUrl() {
        return "jdbc:h2:mem:veilarbdialog-" + (counter++) + ";DB_CLOSE_DELAY=-1;MODE=Oracle";
    }

    public static DataSource build(DbCredentials dbCredentials) {
        return doBuildSingleConnectionDataSource(dbCredentials, false);
    }

    private static DataSource doBuildSingleConnectionDataSource(DbCredentials dbCredentials, boolean migrate) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setSuppressClose(true);
        dataSource.setUrl(dbCredentials.url);
        dataSource.setUsername(dbCredentials.username);
        dataSource.setPassword(dbCredentials.password);
        if (migrate) {
            createTables(dataSource);
        }
        return dataSource;
    }

    private static void createTables(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }
}
