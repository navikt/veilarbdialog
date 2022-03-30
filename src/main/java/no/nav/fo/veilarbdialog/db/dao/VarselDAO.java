package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VarselDAO {

    private final JdbcTemplate jdbc;


    public List<Long> hentDialogerMedUlesteMeldingerEtterSisteVarsel(long graceMillis) {
        final Date grense = new Date(System.currentTimeMillis() - graceMillis);
        return jdbc.queryForList("select DISTINCT d.DIALOG_ID " +
                        "from DIALOG d " +
                        "left join HENVENDELSE h on h.DIALOG_ID = d.DIALOG_ID " +
                        "left join VARSEL v on v.AKTOR_ID = d.AKTOR_ID " +
                        "where h.AVSENDER_TYPE = ? " +
                        "and (d.LEST_AV_BRUKER_TID is null or h.SENDT > d.LEST_AV_BRUKER_TID) " +
                        "and (v.SENDT is null or h.SENDT > v.SENDT) " +
                        "and h.SENDT < ? ",
                Long.class,
                AvsenderType.VEILEDER.name(),
                grense
        );
    }

    public List<String> hentAktorerMedUlesteMeldingerEtterSisteVarsel(long graceMillis) {
        final Date grense = new Date(System.currentTimeMillis() - graceMillis);
        return jdbc.queryForList("select d.AKTOR_ID " +
                        "from DIALOG d " +
                        "left join HENVENDELSE h on h.DIALOG_ID = d.DIALOG_ID " +
                        "left join VARSEL v on v.AKTOR_ID = d.AKTOR_ID " +
                        "where h.AVSENDER_TYPE = ? " +
                        "and (d.LEST_AV_BRUKER_TID is null or h.SENDT > d.LEST_AV_BRUKER_TID) " +
                        "and (v.SENDT is null or h.SENDT > v.SENDT) " +
                        "and h.SENDT < ? " +
                        "group by d.AKTOR_ID",
                String.class,
                AvsenderType.VEILEDER.name(),
                grense
        );
    }

    public void oppdaterSisteVarselForBruker(String aktorId) {
        final int rowsUpdated = jdbc.update("update VARSEL set SENDT = CURRENT_TIMESTAMP where AKTOR_ID = ?" , aktorId);
        if (rowsUpdated == 0) {
            opprettVarselForBruker(aktorId);
        }
    }

    public void setVarselUUIDForParagraf8Dialoger(String aktorId, String varselUUID) {
        jdbc.update("update DIALOG " +
                        "set PARAGRAF8_VARSEL_UUID = ? " +
                        "where PARAGRAF8_VARSEL_UUID is null " +
                        "and ULESTPARAGRAF8VARSEL = 1 " +
                        "and AKTOR_ID = ?",
                varselUUID,
                aktorId);
    }

    public boolean harUlesteUvarsledeParagraf8Henvendelser(String aktorId) {
        try {
            final int unread = Optional
                    .ofNullable(
                            jdbc.queryForObject("select count(AKTOR_ID) " +
                                            "from DIALOG " +
                                            "where ULESTPARAGRAF8VARSEL = 1 " +
                                            "and PARAGRAF8_VARSEL_UUID is null " +
                                            "and AKTOR_ID = ?",
                                    Integer.class,
                                    aktorId))
                    .orElse(0);
            return unread > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    public int hentAntallAktiveDialogerForVarsel(String paragraf8VarselUUID) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject("select count(*) as antall " +
                                    "from DIALOG " +
                                    "where PARAGRAF8_VARSEL_UUID = ? " +
                                    "and ULESTPARAGRAF8VARSEL = 1",
                            Integer.class,
                            paragraf8VarselUUID))
                    .orElse(0);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    public void insertParagraf8Varsel(String aktorid, String varselUuid) {
        jdbc.update("insert into PARAGRAF8VARSEL (UUID, AKTORID, SENDT) values (?, ?, CURRENT_TIMESTAMP)",
                varselUuid,
                aktorid);
    }

    public void revarslingSkalAvsluttes(String paragraf8VarselUUID) {
        jdbc.update("update PARAGRAF8VARSEL set SKALSTOPPES = 1 where UUID = ?",
                paragraf8VarselUUID);
    }

    public List<String> hentRevarslerSomSkalStoppes() {
        return jdbc.queryForList("select UUID from PARAGRAF8VARSEL where SKALSTOPPES = 1", String.class);
    }

    public void markerSomStoppet(String varselUUID) {
        jdbc.update("update PARAGRAF8VARSEL set SKALSTOPPES = 0, DEAKTIVERT = CURRENT_TIMESTAMP where UUID = ? ",
                varselUUID);
    }

    private void opprettVarselForBruker(String aktorId) {
        jdbc.update("INSERT INTO VARSEL (AKTOR_ID, SENDT) VALUES (?, CURRENT_TIMESTAMP)",
                aktorId
        );
    }
}
