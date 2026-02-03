package no.nav.fo.veilarbdialog.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;

import static java.lang.String.format;
import static no.nav.fo.veilarbdialog.config.EnhanceSecureLogsFilter.SECURELOGS_ER_INTERN_BRUKER;
import static no.nav.fo.veilarbdialog.config.EnhanceSecureLogsFilter.SECURELOGS_INNLOGGET_BRUKER_IDENT;

public class SecureRequestLoggerFilter implements Filter {

    private final Logger secureLog = LoggerFactory.getLogger("SecureLog");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String msg = format("status=%s method=%s host=%s path=%s erInternBruker=%s innloggetIdent=%s queryString=%s",
                httpResponse.getStatus(),
                httpRequest.getMethod(),
                httpRequest.getServerName(),
                httpRequest.getRequestURI(),
                MDC.get(SECURELOGS_ER_INTERN_BRUKER),
                MDC.get(SECURELOGS_INNLOGGET_BRUKER_IDENT),
                httpRequest.getQueryString()
        );
        secureLog.info(msg);
    }

    @Override
    public void destroy() {
        MDC.remove(SECURELOGS_ER_INTERN_BRUKER);
        MDC.remove(SECURELOGS_INNLOGGET_BRUKER_IDENT);

        Filter.super.destroy();
    }
}
