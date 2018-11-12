package no.nav.fo.veilarbdialog.db;

import no.nav.sbl.jdbc.DataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import no.nav.sbl.jdbc.Database;

import javax.naming.NamingException;
import javax.sql.DataSource;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class DatabaseContext {

    public static final String DATA_SOURCE_JDNI_NAME = "jdbc/DialogDS";
    public static final String VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME = "VEILARBDIALOGDATASOURCE_URL";
    public static final String VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME = "VEILARBDIALOGDATASOURCE_USERNAME";
    public static final String VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME = "VEILARBDIALOGDATASOURCE_PASSWORD";

    @Bean
    public DataSource dataSource() {
        return DataSourceFactory.dataSource()
                .url(getRequiredProperty(VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME))
                .username(getRequiredProperty(VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME))
                .password(getOptionalProperty(VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME).orElse(""))
                .build();
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
}
