package no.nav.fo.veilarbdialog.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class LoggingErrorController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest request) {

        String uri = request.getRequestURI();
        Map<String, String> headers = new HashMap<>();
        request
                .getHeaderNames()
                .asIterator()
                .forEachRemaining(header -> headers.put(header, request.getHeader(header)));
        log.warn("Request to {} with headers {} caused an error", uri, headers);
        return "error";

    }

    /**
     * Needed to fulfill {@code ErrorController}.
     *
     * @return {@code null}
     */
    @Deprecated
    @Override
    public String getErrorPath() {
        return null;
    }

}
