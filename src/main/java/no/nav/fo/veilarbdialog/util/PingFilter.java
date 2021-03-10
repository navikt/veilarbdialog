package no.nav.fo.veilarbdialog.util;

import org.springframework.http.HttpStatus;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

public class PingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        httpServletResponse.setStatus(HttpStatus.OK.value());
    }

    @Override
    public void destroy() {
    }

}
