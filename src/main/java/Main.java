import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbdialog.ApplicationContext;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbdialog.ApplicationContext.AKTOER_V2_URL_PROPERTY;
import static no.nav.fo.veilarbdialog.ApplicationContext.VEILARBAZUREADPROXY_DISCOVERY_URL_PROPERTY;
import static no.nav.fo.veilarbdialog.ApplicationContext.VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;


public class Main {

    public static void main(String... args) {
        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_URL_PROPERTY));
        setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY));
        setProperty(AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME, getRequiredProperty(VEILARBAZUREADPROXY_DISCOVERY_URL_PROPERTY));
        ApiApp.runApp(ApplicationContext.class, args);
    }

}
