package no.nav.fo.veilarbdialog.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbdialog.rest.DialogRessurs;
import no.nav.fo.veilarbdialog.service.ScheduleRessurs;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
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
@Import({
        AbacContext.class,
        AktorConfig.class,
        CacheConfig.class,
        DatabaseConfig.class,
        FeedConfig.class,
        FeedConsumerConfig.class,
        KvpClientConfig.class,
        MessageQueueConfig.class,
        ServiceConfig.class,
        UnleashConfig.class,
        DialogRessurs.class,
        ScheduleRessurs.class
})
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarbdialog";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String VEILARBOPPFOLGINGAPI_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String AZURE_AD_B2C_DISCOVERY_URL_PROPERTY = "aad_b2c_discovery";
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
    public static final String DIALOGAKTOR_FEED_BRUKERTILGANG_PROPERTY = "dialogaktor.feed.brukertilgang";

    private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

    @Inject
    UnleashService unleashService;

    @Bean
    public VeilarbAbacPepClient pepClient(Pep pep) {

        return VeilarbAbacPepClient.ny()
                .medPep(pep)
                .medSystemUserTokenProvider(()->systemUserTokenProvider.getToken())
                .brukAktoerId(()->unleashService.isEnabled("veilarbdialog.veilarbabac.aktor"))
                .sammenlikneTilgang(()->unleashService.isEnabled("veilarbdialog.veilarbabac.sammenlikn"))
                .foretrekkVeilarbAbacResultat(()->unleashService.isEnabled("veilarbdialog.veilarbabac.foretrekk_veilarbabac"))
                .bygg();
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcLockProvider(ds));
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Inject
    private DataSource dataSource;

    @Override
    public void startup(ServletContext servletContext) {
        setProperty(DIALOGAKTOR_FEED_BRUKERTILGANG_PROPERTY, "srvveilarbportefolje", PUBLIC);
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
