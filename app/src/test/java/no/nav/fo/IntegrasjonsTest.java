package no.nav.fo;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.SubjectHandlerUtils;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.LdapConfig;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.fo.veilarbdialog.ApplicationContext;
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
import javax.naming.NamingException;
import javax.security.auth.Subject;
import java.io.IOException;

import static no.nav.brukerdialog.security.context.SubjectHandlerUtils.setSubject;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.fo.veilarbdialog.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;

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

    protected void setVeilederSubject(String ident) {
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", ThreadLocalSubjectHandler.class.getName());
        SubjectHandlerUtils.SubjectBuilder subjectBuilder = new SubjectHandlerUtils.SubjectBuilder(ident, IdentType.InternBruker);
        Subject subject = subjectBuilder.getSubject();
        subject.getPublicCredentials().add(new OidcCredential(ISSOProvider.getISSOToken()));
        setSubject(subject);
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
            builder.bind(AKTIVITET_DATA_SOURCE_JDNI_NAME, DatabaseTestContext.buildDataSource());
            builder.activate();
        }

    }

    @Before
    public final void fiksJdniOgLdapKonflikt() throws NamingException {
        jndiBean.builder.deactivate();
    }

    @Component
    public static class Request extends MockHttpServletRequest {}

}
