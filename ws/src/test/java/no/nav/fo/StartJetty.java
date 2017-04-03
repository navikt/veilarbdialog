package no.nav.fo;

import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static no.nav.fo.veilarbdialog.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {
    private static final int PORT = 8581;

    public static void main(String[] args) throws Exception {
//        System.setProperty(SubjectHandler.SUBJECTHANDLER_KEY, JettySubjectHandler.class.getName());
//        System.setProperty("java.security.auth.login.config", StartJetty.class.getResource("/login.conf").toExternalForm());
//
//        JAASLoginService jaasLoginService = new JAASLoginService("SAML Realm");
//        jaasLoginService.setLoginModuleName("saml");


        Jetty jetty = usingWar()
                .at("/veilarbdialog-ws")
                .loadProperties("/test.properties")
                .addDatasource(DatabaseTestContext.buildDataSource(), AKTIVITET_DATA_SOURCE_JDNI_NAME)
//                .withLoginService(jaasLoginService)
                .port(PORT)
                .sslPort(PORT + 1)
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
