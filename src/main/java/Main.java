import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbdialog.ApplicationContext;


public class Main {

    public static void main(String... args) {
//        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_URL_PROPERTY));
//        setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY));
        ApiApp.runApp(ApplicationContext.class, args);
    }

}
