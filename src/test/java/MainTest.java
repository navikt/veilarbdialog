import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.testconfig.ApiAppTest;

import static java.lang.System.setProperty;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDbCredentials;
import static no.nav.fo.veilarbdialog.ApplicationContext.APPLICATION_NAME;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME;

public class MainTest {

    private static final String PORT = "8580";

    public static void main(String[] args) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

        DbCredentials dbCredentials = getDbCredentials(APPLICATION_NAME);
        setProperty(VEILARBDIALOGDATASOURCE_URL_PROPERTY_NAME, dbCredentials.getUrl());
        setProperty(VEILARBDIALOGDATASOURCE_USERNAME_PROPERTY_NAME, dbCredentials.getUsername());
        setProperty(VEILARBDIALOGDATASOURCE_PASSWORD_PROPERTY_NAME, dbCredentials.getPassword());

        Main.main(PORT);
    }

}
