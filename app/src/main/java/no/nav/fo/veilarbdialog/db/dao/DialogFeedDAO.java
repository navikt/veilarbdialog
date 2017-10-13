package no.nav.fo.veilarbdialog.db.dao;

import org.springframework.stereotype.Component;

import no.nav.sbl.jdbc.Database;

import javax.inject.Inject;
import java.util.Date;

@Component
public class DialogFeedDAO {
    
    private final Database database;

    @Inject
    public DialogFeedDAO(Database database) {
        this.database = database;
    }

    public Date hentSisteHistoriskeTidspunkt() {
        return database.queryForObject("SELECT TIDSPUNKT_SISTE_ENDRING AS SISTE_HISTORISKE_DATO FROM FEED_METADATA",
                (rs) -> Database.hentDato(rs, "SISTE_HISTORISKE_DATO")
        );
    }
}
