package no.nav.fo.veilarbdialog.actuators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("unleash")
@RequiredArgsConstructor
@Slf4j
public class UnleashHealthCheck implements HealthIndicator {

    private final UnleashService unleashService;

    @Override
    public Health health() {

        if (unleashService.checkHealth().isHealthy()) {
            return Health.up().build();
        }
        return Health.down().build();

    }
}
