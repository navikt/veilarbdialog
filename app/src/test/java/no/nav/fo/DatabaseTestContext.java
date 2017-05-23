package no.nav.fo;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    public static SingleConnectionDataSource buildDataSource() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setSuppressClose(true);
        dataSource.setUrl("jdbc:h2:mem:veilarbdialog;DB_CLOSE_DELAY=-1;MODE=Oracle");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        createTables(dataSource);
        return dataSource;
    }

    private static void createTables(SingleConnectionDataSource singleConnectionDataSource) {
        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbdialogDataSource");
        flyway.setDataSource(singleConnectionDataSource);
        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));
    }

}
