package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.db.Database;
import org.springframework.stereotype.Component;

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
        return database.queryForObject("SELECT MAX(HISTORISK_DATO) AS SISTE_HISTORISKE_DATO FROM DIALOG",
                (rs) -> Database.hentDato(rs, "SISTE_HISTORISKE_DATO")
        );
    }
}
