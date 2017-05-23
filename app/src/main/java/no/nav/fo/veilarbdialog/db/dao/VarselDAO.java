package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.veilarbdialog.db.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;


@Component
public class VarselDAO {

    @Inject
    private Database database;

    @Inject
    DialogDAO dialogDAO;

    public List<String> hentAktorerMedUlesteMeldinger(long graceMillis) {
        Date grense = new Date(System.currentTimeMillis() - graceMillis);
        return database.query("SELECT aktor_id " +
                        "FROM DIALOG d " +
                        "LEFT JOIN HENVENDELSE h on h.dialog_id = d.dialog_id " +
                        "LEFT JOIN VARSEL v on v.aktor_id = d.aktor_id " +
                        "WHERE (d.lest_av_bruker IS NULL OR h.sendt > d.lest_av_bruker) " +
                        "AND (v.sendt IS NULL OR h.sendt > v.sendt) " +
                        "AND h.sendt < ? " +
                        "GROUP BY aktor_id",
                this::mapTilAktorIdListe,
                grense);
    }

    private String mapTilAktorIdListe(ResultSet rs) throws SQLException {
        return rs.getString("aktor_id");
    }

    public void oppdaterSisteVarselForBruker(String aktorId) {
        oppdaterSisteVarselForBruker(aktorId, new Date());
    }

    void oppdaterSisteVarselForBruker(String aktorId, Date date) {
        val rader = database.update("UPDATE VARSEL SET sendt = ? WHERE aktor_id = ?",
                date,
                aktorId
        );

        if (rader == 0) {
            opprettVarselForBruker(aktorId, date);
        }
    }

    private void opprettVarselForBruker(String aktorId, Date date) {
        database.update("INSERT INTO VARSEL (aktor_id, sendt) VALUES (?,?)",
                aktorId,
                date
        );
    }
}
