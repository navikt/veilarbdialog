package no.nav.fo;

import no.nav.fo.veilarbdialog.ApplicationContext;
import no.nav.modig.testcertificates.TestCertificates;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static no.nav.fo.veilarbdialog.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static org.apache.cxf.staxutils.StaxUtils.ALLOW_INSECURE_PARSER;

@ContextConfiguration(classes = {
        ApplicationContext.class,
        IntegrasjonsTest.JndiBean.class,
        IntegrasjonsTest.Request.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource("classpath:test.properties")
@Transactional
public abstract class IntegrasjonsTest {

    @BeforeClass
    public static void testCertificates() {
        TestCertificates.setupKeyAndTrustStore();
    }

    @BeforeClass
    public static void tillatInsecureParser() {
        System.setProperty(ALLOW_INSECURE_PARSER, Boolean.TRUE.toString());
    }

    @BeforeClass
    public static void testProperties() throws IOException {
        System.getProperties().load(IntegrasjonsTest.class.getResourceAsStream("/test.properties"));
    }

    @Component
    public static class JndiBean {

        public JndiBean() throws Exception {
            SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
            builder.bind(AKTIVITET_DATA_SOURCE_JDNI_NAME, DatabaseTestContext.buildDataSource());
            builder.activate();
        }

    }

    @Component
    public static class Request extends MockHttpServletRequest {}

}
