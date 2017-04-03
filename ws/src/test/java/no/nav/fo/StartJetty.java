package no.nav.fo;

import no.nav.modig.core.context.JettySubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.jaas.JAASLoginService;

import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;

public class StartJetty {
    private static final int PORT = 8481;

    public static void main(String[] args) throws Exception {
        System.setProperty(SubjectHandler.SUBJECTHANDLER_KEY, JettySubjectHandler.class.getName());
        System.setProperty("java.security.auth.login.config", StartJetty.class.getResource("/login.conf").toExternalForm());

        JAASLoginService jaasLoginService = new JAASLoginService("SAML Realm");
        jaasLoginService.setLoginModuleName("saml");

        /**
         * Legg på følgende for å teste mot reell database : .addDatasourceByPropertyFile("/db.properties")
         */

        Jetty jetty = usingWar()
                .at("/veilarbdialog-ws")
                .loadProperties("/test.properties")
                //.addDatasourceByPropertyFile("/db.properties")
                .addDatasource(DatabaseTestContext.buildDataSource(), AKTIVITET_DATA_SOURCE_JDNI_NAME)
                .withLoginService(jaasLoginService)
                .port(PORT)
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
