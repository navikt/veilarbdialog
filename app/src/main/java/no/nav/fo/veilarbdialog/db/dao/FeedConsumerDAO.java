package no.nav.fo.veilarbdialog.db.dao;


import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;


@Component
public class FeedConsumerDAO {
    private final Database database;
    private final DateProvider dateProvider;

    @Inject
    public FeedConsumerDAO(Database database, DateProvider dateProvider) {
        this.database = database;
        this.dateProvider = dateProvider;
    }

    public Date hentSisteHistoriskeTidspunkt() {
        return database.queryForObject(
                "SELECT tidspunkt_siste_endring " +
                        "FROM FEED_METADATA",
                (rs) -> Database.hentDato(rs, "tidspunkt_siste_endring")
        );
    }

    void oppdaterSisteHistoriskeTidspunkt() {
        database.update("UPDATE FEED_METADATA SET " +
                "tidspunkt_siste_endring = " + dateProvider.getNow()
        );
    }

}
