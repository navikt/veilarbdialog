import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.QueueManager;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.testconfig.ApiAppTest;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.fo.veilarbdialog.ApplicationContext.*;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.*;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;

public class MainTest {

    private static final String PORT = "8580";
    private static final String SERVICE_USER_ALIAS = "srvveilarbdialog";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String ABAC_PDP_ENDPOINT_ALIAS = "abac.pdp.endpoint";
    private static final String AKTOER_V2_ALIAS = "Aktoer_v2";
    private static final String VEIL_ARB_OPPFOLGING_API_ALIAS = "veilArbOppfolgingAPI";
    private static final String VEILARBLOGIN_REDIRECT_URL_ALIAS = "veilarblogin.redirect-url";
    private static final String VEILARBAZUREADPROXY_DISCOVERY_ALIAS = "veilarbazureadproxy_discovery";
    private static final String AAD_B2C_CLIENTID_ALIAS = "aad_b2c_clientid";
    private static final String AKTIVITETSPLAN_ALIAS = "aktivitetsplan";
    private static final String UNLEASH_API_ALIAS = "unleash-api";
    private static final String MQ_GATEWAY03_ALIAS = "mqGateway03";
    private static final String HENVENDELSE_OPPGAVE_HENVENDELSE_ALIAS = "henvendelse_OPPGAVE.HENVENDELSE";
    private static final String VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_ALIAS = "VARSELPRODUKSJON.STOPP_VARSEL_UTSENDING";
    private static final String VARSELPRODUKSJON_VARSLINGER_ALIAS = "VARSELPRODUKSJON.VARSLINGER";
    private static final String VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_ALIAS = "VARSELPRODUKSJON.BEST_VARSEL_M_HANDLING";

    public static void main(String[] args) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

        ServiceUser serviceUser = getServiceUser(SERVICE_USER_ALIAS, APPLICATION_NAME);
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        DbCredentials dbCredentials = getDbCredentials(APPLICATION_NAME);
        setProperty(VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME, dbCredentials.getUrl());
        setProperty(VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME, dbCredentials.getUsername());
        setProperty(VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME, dbCredentials.getPassword());

        setProperty(STS_URL_KEY, getBaseUrl(SECURITY_TOKEN_SERVICE_ALIAS, FSS));
        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, getRestService(ABAC_PDP_ENDPOINT_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.getPassword());
        setProperty(AKTOER_V2_URL_PROPERTY, getWebServiceEndpoint(AKTOER_V2_ALIAS).getUrl());
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/"); // getRestService(UNLEASH_API_ALIAS, getDefaultEnvironment()).getUrl());

        setProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY, getRestService(VEIL_ARB_OPPFOLGING_API_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(AKTIVITETSPLAN_URL_PROPERTY, getBaseUrl(AKTIVITETSPLAN_ALIAS));

        QueueManager queueManager = getQueueManager(MQ_GATEWAY03_ALIAS);
        setProperty(MQGATEWAY03_HOSTNAME_PROPERTY, queueManager.getHostname());
        setProperty(MQGATEWAY03_PORT_PROPERTY, String.valueOf(queueManager.getPort()));
        setProperty(MQGATEWAY03_NAME_PROPERTY, queueManager.getName());

        setProperty(VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY, getQueue(VARSELPRODUKSJON_VARSLINGER_ALIAS).getName());
        setProperty(VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_QUEUENAME_PROPERTY, getQueue(VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_ALIAS).getName());
        setProperty(VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_QUEUENAME_PROPERTY, getQueue(VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_ALIAS).getName());
        setProperty(HENVENDELSE_OPPGAVE_HENVENDELSE_QUEUENAME_PROPERTY, getQueue(HENVENDELSE_OPPGAVE_HENVENDELSE_ALIAS).getName());

        ServiceUser isso_rp_user = getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService(VEILARBLOGIN_REDIRECT_URL_ALIAS, getDefaultEnvironment()).getUrl();

        setProperty(ISSO_HOST_URL_PROPERTY_NAME, getBaseUrl("isso-host"));
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(ISSO_JWKS_URL_PROPERTY_NAME, getBaseUrl("isso-jwks"));
        setProperty(ISSO_ISSUER_URL_PROPERTY_NAME, getBaseUrl("isso-issuer"));
        setProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, getBaseUrl("isso.isalive", Zone.FSS));
        setProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY, loginUrl);

        ServiceUser aadB2cUser = getServiceUser(AAD_B2C_CLIENTID_ALIAS, APPLICATION_NAME);
        setProperty(VEILARBAZUREADPROXY_DISCOVERY_URL_PROPERTY, getRestService(VEILARBAZUREADPROXY_DISCOVERY_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(AAD_B2C_CLIENTID_USERNAME_PROPERTY, aadB2cUser.getUsername());
        setProperty(AAD_B2C_CLIENTID_PASSWORD_PROPERTY, aadB2cUser.getPassword());

        Main.main(PORT);
    }

}
