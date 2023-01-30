package no.nav.fo.veilarbdialog.config;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao.dab.spring_auth.IPersonService;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
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

    @Bean
    public LeaderElectionClient leaderElectionClient(LockProvider lockProvider) {
        return new ShedLockLeaderElectionClient(lockProvider);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "veilarbdialog");
    }

    @Bean
    IPersonService personService(AktorOppslagClient aktorOppslagClient) {
        return new IPersonService() {
            @NotNull
            @Override
            public Fnr getFnrForAktorId(@NotNull EksternBrukerId eksternBrukerId) {
                if (eksternBrukerId instanceof Fnr fnr) return fnr;
                return aktorOppslagClient.hentFnr(AktorId.of(eksternBrukerId.get()));
            }

            @NotNull
            @Override
            public AktorId getAktorIdForPersonBruker(@NotNull EksternBrukerId eksternBrukerId) {
                if (eksternBrukerId instanceof AktorId aktorId) return aktorId;
                return aktorOppslagClient.hentAktorId(Fnr.of(eksternBrukerId.get()));
            }
        };
    }

}
