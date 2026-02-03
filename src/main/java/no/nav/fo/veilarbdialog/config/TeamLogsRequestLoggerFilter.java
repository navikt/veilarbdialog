package no.nav.fo.veilarbdialog.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static java.lang.String.format;
import static no.nav.util.TeamLog.teamLog;

@Service
@RequiredArgsConstructor
public class TeamLogsRequestLoggerFilter implements Filter {

    private final IAuthService authService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String erInternBruker = Boolean.toString(authService.erInternBruker());
        String innloggetBrukerIdent = authService.getLoggedInnUser().get();

        String msg = format("status=%s method=%s host=%s path=%s erInternBruker=%s innloggetIdent=%s queryString=%s",
                httpResponse.getStatus(),
                httpRequest.getMethod(),
                httpRequest.getServerName(),
                httpRequest.getRequestURI(),
                erInternBruker,
                innloggetBrukerIdent,
                httpRequest.getQueryString()
        );
        teamLog.info(msg);
    }
}
