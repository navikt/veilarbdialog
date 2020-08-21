package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KvpFeedMetadataDAO {

    private final JdbcTemplate jdbc;

    public long hentSisteId() {
        return Optional
                .ofNullable(jdbc.queryForObject("select SISTE_ID from KVP_FEED_METADATA", Long.class))
                .orElse(0L);
    }

    public void oppdaterSisteFeedId(long id) {
        jdbc.update("update KVP_FEED_METADATA set SISTE_ID = ?", id);
    }

}
