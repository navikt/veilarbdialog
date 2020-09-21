package no.nav.fo.veilarbdialog.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${application.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${application.datasource.url}")
    private String url;

    @Value("${application.datasource.username}")
    private String username;

    @Value("${application.datasource.password}")
    private String password;

    @Bean
    DataSource dataSource() {
        log.info("Creating data source with driver {} to {}", driverClassName, url);
        return DataSourceBuilder
                .create()
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

}
