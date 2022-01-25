package no.nav.fo.veilarbdialog.auth;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPepFactory;
import no.nav.common.abac.audit.AuditLogFilterUtils;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.Credentials;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.abac.audit.AuditLogFilterUtils.not;
import static no.nav.common.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PepAuditLogFilterTest {

    private Pep pep;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");
        String url = "http://localhost:" + wireMockRule.port();
        pep = VeilarbPepFactory.get(
                url,
                "",
                "",
                new SpringAuditRequestInfoSupplier(),
                not(AuditLogFilterUtils.pathFilter(path -> path.endsWith("/api/dialog/sistOppdatert")))
        );
        givenAbacPermitResponse();
    }

    @Test
    public void filtrering_av_audit_log_skal_ikke_logge_for_endepunkt_sistOppdatert() {
        List<ILoggingEvent> logsList = logEventsForLogger("AuditLogger");
        mockRequestContextHolder("/veilarbdialog/api/dialog/sistOppdatert");
        pep.harTilgangTilPerson("", ActionId.READ, Fnr.of(""));
        assertEquals(0, logsList.size());
    }

    @Test
    public void filtrering_av_audit_log_skal_logge_for_endepunkt() {
        List<ILoggingEvent> logsList = logEventsForLogger("AuditLogger");
        mockRequestContextHolder("/api/dialog/ikkeSistOppdatert");
        pep.harTilgangTilPerson("", ActionId.READ, Fnr.of(""));
        assertEquals(1, logsList.size());
    }

    private void mockRequestContextHolder(String requestUri) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(CONSUMER_ID_HEADER_NAME)).thenReturn("123");
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn(requestUri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
    }

    private void givenAbacPermitResponse() {
        givenThat(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\n" +
                                "  \"Response\": {\n" +
                                "    \"Decision\": \"Permit\"\n" +
                                "  }\n" +
                                "}")));
    }

    private List<ILoggingEvent> logEventsForLogger(String loggerName) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        return listAppender.list;
    }
}
