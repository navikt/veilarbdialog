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

    @Bean
    public DataSource dataSource() {

        log.info("Creating data source to URL '{}'", url);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(150);
        config.setMinimumIdle(2);

        return migrate(new HikariDataSource(config));
    }

    public static DataSource migrate(DataSource dataSource) {
        log.info("Explicitly migrating {} using Flyway", dataSource);

        var flyway = new Flyway(Flyway.configure()
                .dataSource(dataSource)
                .table("schema_version")
                .validateMigrationNaming(true));

        flyway.migrate();
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
