package no.nav.fo.veilarbdialog;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.ApiApplication.NaisApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

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

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClient(pep, "veilarb", ResourceType.VeilArbPerson);
    }

    @Inject
    private DataSource dataSource;

    @Override
    public void startup(ServletContext servletContext) {
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
