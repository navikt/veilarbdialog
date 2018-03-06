package no.nav.fo.veilarbdialog;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;


import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.fo.DatabaseTestContext;
import no.nav.fo.veilarbdialog.util.VarselMock;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.setProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.fo.veilarbdialog.ApplicationContext.APPLICATION_NAME;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarbdialog.util.StringUtils.of;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

public class StartJetty {

    public static final int PORT = 8580;
    private static final int SSL_PORT = 8585;

    public static void main(String[] args) throws Exception {
        loadPropertiesFile("/environment-local.properties");
        Jetty jetty = setupISSO(usingWar()
                        .at(APPLICATION_NAME)
                        .loadProperties("/environment-test.properties")
                        .addDatasource(createDataSource(), AKTIVITET_DATA_SOURCE_JDNI_NAME)
                        .port(PORT)
                        .sslPort(SSL_PORT)
                , new ISSOSecurityConfig(APPLICATION_NAME)).buildJetty();

        VarselMock.init();
        setupTestContext();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private static void loadPropertiesFile(String propertyFile) {
        Properties properties = new Properties();
        try {
            properties.load(System.class.getResourceAsStream(propertyFile));
        } catch (IOException e) {
            //Ignores non-exisiting file
            return;
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }

    private static DataSource createDataSource() {
        return of(System.getProperty("database"))
                .map(TestEnvironment::valueOf)          
                .map(testEnvironment -> FasitUtils.getDbCredentials(testEnvironment, APPLICATION_NAME))
                .map(DatabaseTestContext::build)  
                .orElseGet(DatabaseTestContext::buildDataSource);
    }

}
