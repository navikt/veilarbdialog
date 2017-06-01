package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.veilarbdialog.db.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;


@Component
public class VarselDAO {

    @Inject
    private Database database;

    @Inject
    DateProvider dateProvider;

    public List<String> hentAktorerMedUlesteMeldinger(long graceMillis) {
        Date grense = new Date(System.currentTimeMillis() - graceMillis);
        return database.query("SELECT d.aktor_id " +
                        "FROM DIALOG d " +
                        "LEFT JOIN HENVENDELSE h on h.dialog_id = d.dialog_id " +
                        "LEFT JOIN VARSEL v on v.aktor_id = d.aktor_id " +
                        "WHERE (d.lest_av_bruker_tid IS NULL OR h.sendt > d.lest_av_bruker_tid) " +
                        "AND (v.sendt IS NULL OR h.sendt > v.sendt) " +
                        "AND h.sendt < ? " +
                        "GROUP BY d.aktor_id",
                (rs) -> rs.getString("aktor_id"),
                grense);
    }

    public void oppdaterSisteVarselForBruker(String aktorId) {
        val rader = database.update("UPDATE VARSEL SET sendt = " + dateProvider.getNow() + " WHERE aktor_id = ?",
                aktorId
        );

        if (rader == 0) {
            opprettVarselForBruker(aktorId);
        }
    }

    private void opprettVarselForBruker(String aktorId) {
        database.update("INSERT INTO VARSEL (aktor_id, sendt) VALUES (?," + dateProvider.getNow() + ")",
                aktorId
        );
    }
}
