package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbdialog.db.dao.DBKonstanter.PARAGAF8_VARSEL_UUID;

@Component
public class VarselDAO {

    @Inject
    private Database database;

    @Inject
    private DateProvider dateProvider;

    public List<String> hentAktorerMedUlesteMeldingerEtterSisteVarsel(long graceMillis) {
        Date grense = new Date(System.currentTimeMillis() - graceMillis);
        return database.query("SELECT d.aktor_id " +
                        "FROM DIALOG d " +
                        "LEFT JOIN HENVENDELSE h on h.dialog_id = d.dialog_id " +
                        "LEFT JOIN VARSEL v on v.aktor_id = d.aktor_id " +
                        "WHERE h.avsender_type = ? " +
                        "AND (d.lest_av_bruker_tid IS NULL OR h.sendt > d.lest_av_bruker_tid) " +
                        "AND (v.sendt IS NULL OR h.sendt > v.sendt) " +
                        "AND h.sendt < ? " +
                        "GROUP BY d.aktor_id",
                (rs) -> rs.getString("aktor_id"),
                AvsenderType.VEILEDER.name(),
                grense);
    }

    public void oppdaterSisteVarselForBruker(String aktorId) {
        int rader = database.update("UPDATE VARSEL SET sendt = " + dateProvider.getNow() + " WHERE aktor_id = ?",
                aktorId
        );

        if (rader == 0) {
            opprettVarselForBruker(aktorId);
        }
    }

    public void setVarselUUIDForParagraf8Dialoger(String aktorId, String varselUUID) {
        database.update("UPDATE DIALOG " +
                "SET paragraf8_varsel_uuid = ? " +
                "WHERE paragraf8_varsel_uuid IS NULL " +
                "AND ulestParagraf8Varsel = 1 " +
                "AND aktor_id = ?",
                varselUUID,
                aktorId);
    }

    public boolean harUlesteUvarsledeParagraf8Henvendelser(String aktorId) {
        List<String> aktors = database.query("SELECT aktor_id " +
                "FROM DIALOG " +
                "where ulestParagraf8Varsel = 1, " +
                "and paragraf8_varsel_uuid IS NULL " +
                "and aktor_id = ?",
                (rs) -> rs.getString("aktor_id"),
                aktorId);

        return !aktors.isEmpty();
    }

    public int hentAntallAktiveDialogerForVarsel(String paragraf8VarselUUID) {
        return database.queryForObject("select count(*) as antall " +
                        "from DILAOG " +
                        "where " + PARAGAF8_VARSEL_UUID + " = ? ",
                rs -> rs.getInt("antall"),
                paragraf8VarselUUID);
    }

    public void insertParagraf8Varsel(String aktorid, String varselUuid) {
        database.update("INSERT INTO PARAGRAF8VARSEL (uuid, aktorid, sendt) VALUES (?, ?, "+ dateProvider.getNow() + ")",
                varselUuid, aktorid);
    }

    public void revarslingSkalAvsluttes(String paragraf8VarselUUID) {
        database.update("update PARAGRAF8VARSEL set skalStoppes = 1 where "+ PARAGAF8_VARSEL_UUID + " = ?", paragraf8VarselUUID);
    }

    public List<String> hentRevarslerSomSkalStoppes() {
        return database.query("select " + PARAGAF8_VARSEL_UUID + " from PARAGRAF8VARSEL where skalStoppes = 1", rs -> rs.getString(PARAGAF8_VARSEL_UUID));
    }

    public void markerSomStoppet(String varselUUID) {
        database.update("update PARAGRAF8VARSEL set skalStoppes = 0, deaktivert = " + dateProvider.getNow() +
                " where " + PARAGAF8_VARSEL_UUID + " = ? ", varselUUID);
    }

    private void opprettVarselForBruker(String aktorId) {
        database.update("INSERT INTO VARSEL (aktor_id, sendt) VALUES (?," + dateProvider.getNow() + ")",
                aktorId
        );
    }
}
