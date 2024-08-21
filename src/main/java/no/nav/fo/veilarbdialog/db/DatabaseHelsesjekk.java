package no.nav.fo.veilarbdialog.db;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseHelsesjekk implements HealthCheck {

    private final JdbcTemplate jdbc;

    @Override
    public HealthCheckResult checkHealth() {
//        try {
//            jdbc.queryForObject("select 1", Long.class);
//        } catch (DataAccessException e) {
//            return HealthCheckResult.unhealthy(e);
//        }
        return HealthCheckResult.healthy();
    }

}
