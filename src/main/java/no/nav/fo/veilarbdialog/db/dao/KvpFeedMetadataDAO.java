package no.nav.fo.veilarbdialog.db.dao;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import no.nav.sbl.jdbc.Database;

@Component
public class KvpFeedMetadataDAO {

    private final Database database;

    @Inject
    public KvpFeedMetadataDAO(Database database) {
        this.database = database;
    }

    public long hentSisteId() {
        return database.queryForObject(
                "SELECT SISTE_ID " +
                        "FROM KVP_FEED_METADATA",
                (rs) -> rs.getLong("SISTE_ID")
        );
    }

    public void oppdaterSisteFeedId(long id) {
        database.update(
                "UPDATE KVP_FEED_METADATA SET SISTE_ID = ?", 
                id
        );
    }

}
