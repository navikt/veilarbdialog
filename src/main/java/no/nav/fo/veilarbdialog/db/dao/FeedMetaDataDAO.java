package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class FeedMetaDataDAO {

    private final JdbcTemplate jdbc;

    public Date hentSisteLestTidspunkt() {
        try {
            Timestamp timestamp = jdbc.queryForObject("select TIDSPUNKT_SISTE_ENDRING from FEED_METADATA",
                    Timestamp.class);
            return timestamp == null ? null : Date.from(timestamp.toInstant());

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void oppdaterSisteLest(Date date) {
        jdbc.update("update FEED_METADATA set TIDSPUNKT_SISTE_ENDRING = ?",
                date);
    }

}
