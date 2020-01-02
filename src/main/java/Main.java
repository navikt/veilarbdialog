import no.nav.apiapp.ApiApp;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.common.utils.NaisUtils;
import no.nav.fo.veilarbdialog.config.ApplicationConfig;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbdialog.config.ApplicationConfig.*;
import static no.nav.fo.veilarbdialog.config.DatabaseConfig.VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME;
import static no.nav.fo.veilarbdialog.config.DatabaseConfig.VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {
        readFromConfigMap();

        NaisUtils.Credentials serviceUser = getCredentials("service_user");

        //ABAC
        System.setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //CXF
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //OIDC
        System.setProperty(SecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        System.setProperty(StsSecurityConstants.STS_URL_KEY, getRequiredProperty(SECURITYTOKENSERVICE_URL));
        System.setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_ENDPOINTURL));
        System.setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(REDIRECT_URL_PROPERTY));
        System.setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, getRequiredProperty(ABAC_PDP_ENDPOINT_URL));
        System.setProperty(AKTOERREGISTER_API_V1_URL, getRequiredProperty(AKTOERREGISTER_API_V1_URL));

        NaisUtils.Credentials oracleCreds = getCredentials("oracle_creds");
        System.setProperty(VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME, oracleCreds.username);
        System.setProperty(VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME, oracleCreds.password);

        ApiApp.runApp(ApplicationConfig.class, args);
    }


    private static void readFromConfigMap() {
        NaisUtils.addConfigMapToEnv("pto-config",
                "SECURITYTOKENSERVICE_URL",
                "ABAC_PDP_ENDPOINT_URL",
                "ABAC_PDP_ENDPOINT_DESCRIPTION",
                "AAD_B2C_DISCOVERY_URL",
                "ISSO_HOST_URL",
                "ISSO_JWKS_URL",
                "ISSO_ISSUER_URL",
                "ISSO_ISALIVE_URL",
                "VEILARBLOGIN_REDIRECT_URL_DESCRIPTION",
                "VEILARBLOGIN_REDIRECT_URL_URL",
                "AKTOER_V2_SECURITYTOKEN",
                "AKTOER_V2_ENDPOINTURL",
                "AKTOER_V2_WSDLURL",
                "LOGINSERVICE_OIDC_CALLBACKURI",
                "LOGINSERVICE_OIDC_DISCOVERYURI",
                "UNLEASH_API_URL",
                "AKTOERREGISTER_API_V1_URL",
                "SECURITY_TOKEN_SERVICE_OPENID_CONFIGURATION_URL",
                "VEILARBOPPFOLGINGAPI_URL",
                "AKTIVITETSPLAN_URL",
                "MQGATEWAY03_NAME",
                "MQGATEWAY03_HOSTNAME",
                "MQGATEWAY03_PORT",
                "VARSELPRODUKSJON_VARSLINGER_QUEUENAME",
                "VARSELPRODUKSJON_BEST_VARSEL_M_HANDLING_QUEUENAME",
                "VARSELPRODUKSJON_STOPP_VARSEL_UTSENDING_QUEUENAME",
                "HENVENDELSE_OPPGAVE_HENVENDELSE_QUEUENAME"
        );
    }

}
