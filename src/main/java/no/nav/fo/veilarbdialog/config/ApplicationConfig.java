package no.nav.fo.veilarbdialog.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

// TODO: This is no longer an "application configuration", but rather a "LockConfig", or "ScheduleRessursConfig". Rename and relocate accordingly.
@Configuration
@EnableTransactionManagement
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcTemplateLockProvider(ds));
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }
}
