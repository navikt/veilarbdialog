package no.nav.fo;

import no.nav.apiapp.util.StringUtils;
import no.nav.dialogarena.config.fasit.DbCredentials;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    static{
        setProperty(VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME, inMemoryUrl());
        setProperty(VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME, "sa");
        setProperty(VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME, "");
    }

    private static int counter;

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
//        flyway.setLocations("db/migration/veilarbdialogDataSource");
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    public static DataSource buildMultiDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(inMemoryUrl());
        dataSource.setUser("sa");
        dataSource.setPassword("");
        createTables(dataSource);
        return dataSource;

    }

}
