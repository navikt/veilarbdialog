package no.nav.fo.veilarbdialog;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableScheduling
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbdialog")
@Import({AbacContext.class, AktorConfig.class})
public class ApplicationContext implements ApiApplication {

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClient(pep, "veilarb", ResourceType.VeilArbPerson);
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(DataSource ds) {
            return new DefaultLockingTaskExecutor(new JdbcLockProvider(ds));
    }

}
