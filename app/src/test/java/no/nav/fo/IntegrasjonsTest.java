package no.nav.fo;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.fo.feed.consumer.FeedPoller;
import no.nav.fo.veilarbdialog.ApplicationContext;
import no.nav.fo.veilarbdialog.db.dao.DateProvider;
import no.nav.testconfig.ApiAppTest;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.NamingException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.function.Supplier;

import static no.nav.fo.veilarbdialog.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static no.nav.testconfig.ApiAppTest.setupTestContext;
import static org.mockito.Mockito.mock;
import static org.springframework.util.ReflectionUtils.setField;

public abstract class IntegrasjonsTest {

    public static final String APPLICATION_NAME = "veilarbdialog";

    protected static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static PlatformTransactionManager platformTransactionManager;
    private TransactionStatus transactionStatus;

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

    @Before
    @BeforeEach
    public void setupDateProvider() {
        changeDateProvider(IntegrasjonsTest::timestampFromSystemTime);
    }

    @SneakyThrows
    protected void changeDateProvider(Supplier<String> timestampProvider) {
        Field providerField = DateProvider.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        setField(providerField, null, timestampProvider);
    }

    @AfterClass
    @AfterAll
    public static void shutdownPolling() {
        FeedPoller.shutdown();
    }

    @SneakyThrows
    @BeforeAll
    @BeforeClass
    public static void setupContext() {
        DevelopmentSecurity.setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(APPLICATION_NAME));
        setupTestContext(ApiAppTest.Config.builder()
                .applicationName(APPLICATION_NAME)
                .build()
        );
        
        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(
                ApplicationContext.class,
                IntegrasjonsTest.JndiBean.class,
                IntegrasjonsTest.Request.class
        );
        annotationConfigApplicationContext.start();
        platformTransactionManager = getBean(PlatformTransactionManager.class);
    }

    static String timestampFromSystemTime() {
        return String.format("\'%s\'", new Timestamp(System.currentTimeMillis()));
    }

    protected void setVeilederSubject(String ident) {
        subjectRule.setSubject(new Subject(ident, IdentType.InternBruker, SsoToken.oidcToken(ISSOProvider.getISSOToken())));
    }

    @Component
    public static class JndiBean {

        private final SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();

        public JndiBean() throws Exception {
            builder.bind("java:/jboss/jms/VARSELPRODUKSJON.VARSLINGER", mock(Destination.class));
            builder.bind("java:jboss/mqConnectionFactory", mock(ConnectionFactory.class));
            builder.bind(AKTIVITET_DATA_SOURCE_JDNI_NAME, DatabaseTestContext.buildMultiDataSource());
            builder.activate();
        }

    }

    @BeforeEach
    @Before
    public final void fiksJndiOgLdapKonflikt() throws NamingException {
        getBean(JndiBean.class).builder.deactivate();
    }

    @BeforeEach
    @Before
    public void injectAvhengigheter() {
        annotationConfigApplicationContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @BeforeEach
    @Before
    public void startTransaksjon() {
        transactionStatus = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
    }

    @AfterEach
    @After
    public void rollbackTransaksjon() {
        platformTransactionManager.rollback(transactionStatus);
    }

    @Component
    public static class Request extends MockHttpServletRequest {
    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }


}
