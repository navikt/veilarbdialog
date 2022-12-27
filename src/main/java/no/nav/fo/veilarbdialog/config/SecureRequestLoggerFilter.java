package no.nav.fo.veilarbdialog.config;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.auth.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class SecureRequestLoggerFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger("SecureLog");

    private final AuthService authService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        filterChain.doFilter(servletRequest, servletResponse);

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        Optional<String> innloggetBrukerIdent = authService.getIdent();

        String msg = format("status=%s method=%s host=%s path=%s erInternBruker=%s innloggetIdent=%s queryString=%s",
                httpResponse.getStatus(),
                httpRequest.getMethod(),
                httpRequest.getServerName(),
                httpRequest.getRequestURI(),
                authService.erInternBruker(),
                innloggetBrukerIdent.orElse(null),
                httpRequest.getQueryString()
        );
        log.info(msg);

    }

}
