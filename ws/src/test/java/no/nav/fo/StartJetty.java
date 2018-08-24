package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.fo.veilarbdialog.util.VarselMock;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static no.nav.fo.veilarbdialog.db.DatabaseContext.DATA_SOURCE_JDNI_NAME;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {
    private static final int PORT = 8581;

    public static final String APPLICATION_NAME = "veilarbdialog";
    
    public static void main(String[] args) throws Exception {
        Jetty jetty = DevelopmentSecurity.setupSamlLogin(usingWar()
                        .at("/veilarbdialog-ws")
                        .addDatasource(DatabaseTestContext.buildDataSource(), DATA_SOURCE_JDNI_NAME)
                        .port(PORT)
                        .sslPort(PORT + 1)
                , new DevelopmentSecurity.SamlSecurityConfig(APPLICATION_NAME)
        ).buildJetty();

        VarselMock.init();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }
}
