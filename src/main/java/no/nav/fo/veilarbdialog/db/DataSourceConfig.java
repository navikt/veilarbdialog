package no.nav.fo.veilarbdialog.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${application.datasource.url}")
    private String url;

    @Value("${application.datasource.username}")
    private String username;

    @Value("${application.datasource.password}")
    private String password;

    @Value("${application.datasource.flyway.url}")
    private String jdbcUrlForFlyway;

    @Value("${application.datasource.flyway.username}")
    private String usernameForFlyway;

    @Value("${application.datasource.flyway.password}")
    private String passwordForFlyway;

    @Bean
    public DataSource dataSource() {

        log.info("Creating data source");

        HikariConfig config = new HikariConfig();
        config.setSchema("veilarbdialog");
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(150);
        config.setMinimumIdle(2);
        var dataSource = new HikariDataSource(config);

        // TODO: Delete hikariDataSource for flyway after migration to GCP
        HikariConfig configForFlyway = new HikariConfig();
        configForFlyway.setSchema("veilarbdialog");
        configForFlyway.setJdbcUrl(jdbcUrlForFlyway);
        configForFlyway.setUsername(usernameForFlyway);
        configForFlyway.setPassword(passwordForFlyway);
        configForFlyway.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        var dataSourceForFlyway = new HikariDataSource(configForFlyway);

        migrate(dataSourceForFlyway);

        return dataSource;
    }

    public static void migrate(DataSource dataSource) {
        log.info("Explicitly migrating {} using Flyway", dataSource);

        var flyway = new Flyway(Flyway.configure()
                .dataSource(dataSource)
                .table("schema_version")
                .validateMigrationNaming(true));

        flyway.migrate();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
