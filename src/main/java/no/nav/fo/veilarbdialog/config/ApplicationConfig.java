package no.nav.fo.veilarbdialog.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableScheduling
public class ApplicationConfig {

    public static final String APPLICATION_NAME = "veilarbdialog";
    public static final String VEILARBOPPFOLGINGAPI_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String ARBEIDSRETTET_DIALOG_URL_PROPERTY = "ARBEIDSRETTET_DIALOG_URL";
    public static final String MQGATEWAY03_HOSTNAME_PROPERTY = "MQGATEWAY03_HOSTNAME";
    public static final String MQGATEWAY03_PORT_PROPERTY = "MQGATEWAY03_PORT";
    public static final String MQGATEWAY03_NAME_PROPERTY = "MQGATEWAY03_NAME";
    public static final String VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_VARSLINGER_QUEUENAME";
    public static final String VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_QUEUENAME";
    public static final String VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_QUEUENAME";
    public static final String HENVENDELSE_OPPGAVE_HENVENDELSE_QUEUENAME_PROPERTY = "HENVENDELSE_OPPGAVE_HENVENDELSE_QUEUENAME";

    public static final String VEILARB_KASSERING_IDENTER_PROPERTY = "VEILARB_KASSERING_IDENTER";
    public static final String DIALOGAKTOR_FEED_BRUKERTILGANG_PROPERTY = "dialogaktor.feed.brukertilgang";


    public static final String AKTOER_V2_ENDPOINTURL = "AKTOER_V2_ENDPOINTURL";
    public static final String REDIRECT_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String SECURITYTOKENSERVICE_URL = "SECURITYTOKENSERVICE_URL";

    @Value("${application.abac.url")
    private String abacUrl;

    public static final String AKTOERREGISTER_API_V1_URL = "AKTOERREGISTER_API_V1_URL";

    @Bean
    public Pep pep(Credentials systemUser) {
        return new VeilarbPep(abacUrl, systemUser.username, systemUser.password);
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcTemplateLockProvider(ds));
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

/*    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        *//*
        .sts()
        no.nav.modig.security.sts.url = either sysprop no.nav.modig.security.sts.url (itself) or sysprop SECURITYTOKENSERVICE_URL
        no.nav.modig.security.systemuser.username = SRV<appname>_USERNAME
        no.nav.modig.security.systemuser.password = SRV<appname>_PASSWORD
         *//*
        //OidcAuthenticator.fromConfig()

        apiAppConfigurator
                .sts()
                .azureADB2CLogin()
                .issoLogin()
        ;
    }*/

    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }

}
