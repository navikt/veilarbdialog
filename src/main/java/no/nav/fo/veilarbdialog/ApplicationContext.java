package no.nav.fo.veilarbdialog;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.apiapp.ApiApplication.NaisApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

@Configuration
@EnableTransactionManagement
@EnableScheduling
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbdialog")
@Import({AbacContext.class, AktorConfig.class})
public class ApplicationContext implements NaisApiApplication {

    public static final String APPLICATION_NAME = "veilarbdialog";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String VEILARBOPPFOLGINGAPI_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBAZUREADPROXY_DISCOVERY_URL_PROPERTY = "VEILARBAZUREADPROXY_DISCOVERY_URL";
    public static final String AAD_B2C_CLIENTID_USERNAME_PROPERTY = "AAD_B2C_CLIENTID_USERNAME";
    public static final String AAD_B2C_CLIENTID_PASSWORD_PROPERTY = "AAD_B2C_CLIENTID_PASSWORD";
    public static final String AKTIVITETSPLAN_URL_PROPERTY = "AKTIVITETSPLAN_URL";
    public static final String MQGATEWAY03_HOSTNAME_PROPERTY = "MQGATEWAY03_HOSTNAME";
    public static final String MQGATEWAY03_PORT_PROPERTY = "MQGATEWAY03_PORT";
    public static final String MQGATEWAY03_NAME_PROPERTY = "MQGATEWAY03_NAME";
    public static final String VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_VARSLINGER_QUEUENAME";
    public static final String VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_QUEUENAME";
    public static final String VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_QUEUENAME";
    public static final String HENVENDELSE_OPPGAVE_HENVENDELSE_QUEUENAME_PROPERTY = "HENVENDELSE_OPPGAVE_HENVENDELSE_QUEUENAME";
    public static final String VEILARB_KASSERING_IDENTER_PROPERTY = "VEILARB_KASSERING_IDENTER";
    public static final String DIALOGAKTOR_FEED_BRUKERTILGANG_PROPERTY = "DIALOGAKTOR_FEED_BRUKERTILGANG";

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClient(pep, "veilarb", ResourceType.VeilArbPerson);
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcLockProvider(ds));
    }


    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Override
    public void startup(ServletContext servletContext) {
        setProperty(DIALOGAKTOR_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbportefolje", PUBLIC);

        jdbcTemplate.update("UPDATE \"schema_version\" SET \"checksum\"=153834897 WHERE \"version\" = '1.12'");
        jdbcTemplate.update("UPDATE \"schema_version\" SET \"checksum\"=1010501922 WHERE \"version\" = '1.25'");

        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .sts()
                .azureADB2CLogin()
                .issoLogin()
        ;
    }
}
