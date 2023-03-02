package no.nav.fo.veilarbdialog.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * Needed since some clients apparently ping us instead of using retries or similar.
 * <p>
 * Will forward to the health actuator's ping indicator, which is what clients shoud be using anyways.
 */
@Controller
@Slf4j
public class PingController {

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String basePath;

    @GetMapping("/api/ping")
    public ModelAndView forwardToPing(HttpServletRequest request) {

        String url = "forward:/" + basePath + "/health/ping";
        if (log.isDebugEnabled()) {
            Map<String, String> headers = new HashMap<>();
            request
                    .getHeaderNames()
                    .asIterator()
                    .forEachRemaining(header -> headers.put(header, request.getHeader(header)));
            log.debug("Forwarding request to {} with headers {} to {}", request.getRequestURI(), headers, url);
        }
        return new ModelAndView(url);

    }

}
