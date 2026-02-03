package no.nav.fo.veilarbdialog.config;

import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EnhanceTeamLogsFilter implements Filter {

    private final IAuthService authService;

    public static final String TEAMLOGS_ER_INTERN_BRUKER = "TeamLogsFilter.erInternBruker";
    public static final String TEAMLOGS_INNLOGGET_BRUKER_IDENT = "TeamLogsFilter.innloggetBrukerIdent";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String erInternBruker = Boolean.toString(authService.erInternBruker());
        String innloggetBrukerIdent = authService.getLoggedInnUser().get();

        MDC.put(TEAMLOGS_ER_INTERN_BRUKER, erInternBruker);
        MDC.put(TEAMLOGS_INNLOGGET_BRUKER_IDENT, innloggetBrukerIdent);

        filterChain.doFilter(servletRequest, servletResponse);
    }

}
