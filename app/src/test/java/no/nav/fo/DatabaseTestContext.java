package no.nav.fo;

import no.nav.apiapp.util.StringUtils;
import no.nav.dialogarena.config.fasit.DbCredentials;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    private static int counter;

    public static DataSource buildDataSource() {
        String url = StringUtils.of(System.getProperty("database.url")).orElse(imMemoryUrl());
        return doBuildSingleConnectionDataSource(new DbCredentials()
                        .setUrl(url)
                        .setUsername("sa")
                        .setPassword(""),
                true
        );
    }

    private static String imMemoryUrl() {
        return "jdbc:h2:mem:veilarbdialog-" + (counter++) + ";DB_CLOSE_DELAY=-1;MODE=Oracle";
    }

    public static DataSource build(DbCredentials dbCredentials) {
        return doBuildSingleConnectionDataSource(dbCredentials,false);
    }

    private static DataSource doBuildSingleConnectionDataSource(DbCredentials dbCredentials, boolean migrate) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setSuppressClose(true);
        dataSource.setUrl(dbCredentials.url);
        dataSource.setUsername(dbCredentials.username);
        dataSource.setPassword(dbCredentials.password);
        if (migrate){
            createTables(dataSource);
        }
        return dataSource;
    }

    private static void createTables(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbdialogDataSource");
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    public static DataSource buildMultiDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(imMemoryUrl());
        dataSource.setUser("sa");
        dataSource.setPassword("");
        createTables(dataSource);
        return dataSource;

    }

}
