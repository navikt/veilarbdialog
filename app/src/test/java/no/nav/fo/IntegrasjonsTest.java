package no.nav.fo;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.SubjectHandlerUtils;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.fo.veilarbdialog.ApplicationContext;
import no.nav.fo.veilarbdialog.db.dao.DateProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.function.Supplier;

import static no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.brukerdialog.security.context.SubjectHandlerUtils.setSubject;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static org.mockito.Mockito.mock;
import static org.springframework.util.ReflectionUtils.setField;

@ContextConfiguration(classes = {
        ApplicationContext.class,
        IntegrasjonsTest.JndiBean.class,
        IntegrasjonsTest.Request.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource("classpath:test.properties")
@Transactional
public abstract class IntegrasjonsTest {

    @Inject
    private JndiBean jndiBean;

    @Before
    @SneakyThrows
    public void setupDateProvider() {
        Field providerField = DateProvider.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        setField(providerField, null, (Supplier<String>) IntegrasjonsTest::timestampFromSystemTime);
    }

    static String timestampFromSystemTime() {
        return String.format("\'%s\'", new Timestamp(System.currentTimeMillis()));
    }

    protected void setVeilederSubject(String ident) {
        setProperty(SUBJECTHANDLER_KEY, ThreadLocalSubjectHandler.class.getName());
        setSubject(new SubjectHandlerUtils.SubjectBuilder(ident, IdentType.InternBruker, ISSOProvider.getISSOToken()).getSubject());
    }

    @BeforeClass
    public static void setupIntegrationTestSecurity() {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarbdialog", "veilarbdialog", "t6"));
    }

    @BeforeClass
    public static void setupLdap() {
        DevelopmentSecurity.configureLdap(FasitUtils.getLdapConfig("ldap", "veilarbdialog", "t6"));
    }

    @BeforeClass
    public static void testProperties() throws IOException {
        System.getProperties().load(IntegrasjonsTest.class.getResourceAsStream("/test.properties"));
    }

    @Component
    public static class JndiBean {

        private final SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();

        public JndiBean() throws Exception {
            builder.bind("java:/jboss/jms/VARSELPRODUKSJON.VARSLINGER", mock(Destination.class));
            builder.bind("java:jboss/mqConnectionFactory", mock(ConnectionFactory.class));
            builder.bind(AKTIVITET_DATA_SOURCE_JDNI_NAME, DatabaseTestContext.buildDataSource());
            builder.activate();
        }

    }

    @Before
    public final void fiksJndiOgLdapKonflikt() throws NamingException {
        jndiBean.builder.deactivate();
    }

    @Component
    public static class Request extends MockHttpServletRequest {}

}
