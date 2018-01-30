package no.nav.fo;

import no.nav.apiapp.util.StringUtils;
import no.nav.dialogarena.config.fasit.DbCredentials;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    private static int counter;

    public static SingleConnectionDataSource buildDataSource() {
        String url = StringUtils.of(System.getProperty("database.url"))
                .orElse("jdbc:h2:mem:veilarbdialog-" + (counter++) + ";DB_CLOSE_DELAY=-1;MODE=Oracle");
        return doBuild(new DbCredentials()
                        .setUrl(url)
                        .setUsername("sa")
                        .setPassword(""),
                true
        );
    }

    public static SingleConnectionDataSource build(DbCredentials dbCredentials) {
        return doBuild(dbCredentials,false);
    }

    private static SingleConnectionDataSource doBuild(DbCredentials dbCredentials, boolean migrate) {
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

    private static void createTables(SingleConnectionDataSource singleConnectionDataSource) {
        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbdialogDataSource");
        flyway.setDataSource(singleConnectionDataSource);
        flyway.migrate();
    }

}
