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
                "SELECT tidspunkt_siste_endring " +
                        "FROM FEED_METADATA",
                (rs) -> Database.hentDato(rs, "tidspunkt_siste_endring")
        );
    }

    public void oppdaterSisteLest(Date date) {
        database.update(
                "UPDATE FEED_METADATA SET tidspunkt_siste_endring = ?", 
                date
        );
    }

}
