package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class FeedMetaDataDAO {

    private final JdbcTemplate jdbc;

    public ZonedDateTime hentSisteLestTidspunkt() {
        return jdbc.queryForObject("select tidspunkt_siste_endring from FEED_METADATA",
                ZonedDateTime.class);
    }

    public void oppdaterSisteLest(Date date) {
        jdbc.update("update FEED_METADATA set tidspunkt_siste_endring = ?",
                date);
    }

}
