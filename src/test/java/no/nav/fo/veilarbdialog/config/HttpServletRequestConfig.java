package no.nav.fo.veilarbdialog.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Workaround for wonky handling of {@code HttpServletRequest} for getting fnr/aktorid parameters.
 */
@TestConfiguration
public class HttpServletRequestConfig {

    private final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

    @PostConstruct
    private void postConstruct () {
        when(httpServletRequest.getParameter("fnr")).thenReturn("112233456789");
        when(httpServletRequest.getParameter("aktorid")).thenReturn("12345");
    }

    @Primary
    @Bean
    HttpServletRequest httpServletRequest() {
        return httpServletRequest;
    }

}
