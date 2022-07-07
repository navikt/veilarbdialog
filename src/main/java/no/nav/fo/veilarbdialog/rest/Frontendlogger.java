package no.nav.fo.veilarbdialog.rest;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logger")
public class Frontendlogger {
    private final MetricsClient metricsClient;

    @PostMapping("/event")
    public void skrivEventTilInflux(@RequestBody FrontendEvent event) {
        Event toInflux = new Event(event.name + ".event");
        if (event.getTags() != null) {
            event.getTags().forEach(toInflux::addTagToReport);
        }
        if (event.getFields() != null) {
            event.getFields().forEach(toInflux::addFieldToReport);
        }
        toInflux.getTags().put("environment", isProduction().orElse(false) ? "p" : "q1");

        metricsClient.report(toInflux);
    }

    @Data
    @Accessors(chain = true)
    static class FrontendEvent {
        String name;
        Map<String, Object> fields;
        Map<String, String> tags;
    }
}
