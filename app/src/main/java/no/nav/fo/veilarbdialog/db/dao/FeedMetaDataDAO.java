package no.nav.fo.veilarbdialog.db.dao;


import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;


@Component
public class FeedMetaDataDAO {
    private final Database database;

    @Inject
    public FeedMetaDataDAO(Database database) {
        this.database = database;
    }

    public Date hentSisteLestTidspunkt() {
        return database.queryForObject(
                "SELECT SISTE_OPPFOLGING_ID " +
                        "FROM FEED_METADATA",
                (rs) -> Database.hentDato(rs, "SISTE_OPPFOLGING_ID")
        );
    }

    public void oppdaterSisteLest(Date date) {
        database.update(
                "UPDATE FEED_METADATA SET SISTE_OPPFOLGING_ID = ?", 
                date
        );
    }

}
