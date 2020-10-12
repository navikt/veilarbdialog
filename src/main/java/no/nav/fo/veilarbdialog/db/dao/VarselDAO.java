package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VarselDAO {

    private final JdbcTemplate jdbc;
    private final DateProvider dateProvider;

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
        final int rowsUpdated = jdbc.update("update VARSEL set SENDT = ? where AKTOR_ID = ?", dateProvider.getNow(), aktorId);
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
        final int unread = Optional.ofNullable(
                jdbc.queryForObject("select count(AKTOR_ID) " +
                                "from DIALOG " +
                                "where ULESTPARAGRAF8VARSEL = 1 " +
                                "and PARAGRAF8_VARSEL_UUID is null " +
                                "and AKTOR_ID = ?",
                        Integer.class,
                        aktorId))
                .orElse(0);
        return unread > 0;
    }

    public int hentAntallAktiveDialogerForVarsel(String paragraf8VarselUUID) {
        return Optional.ofNullable(
                jdbc.queryForObject("select count(*) as antall " +
                                "from DIALOG " +
                                "where PARAGRAF8_VARSEL_UUID = ? " +
                                "and ULESTPARAGRAF8VARSEL = 1",
                        Integer.class,
                        paragraf8VarselUUID))
                .orElse(0);
    }

    public void insertParagraf8Varsel(String aktorid, String varselUuid) {
        jdbc.update("insert into PARAGRAF8VARSEL (UUID, AKTORID, SENDT) values (?, ?, " + dateProvider.getNow() + ")",
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
        jdbc.update("update PARAGRAF8VARSEL set SKALSTOPPES = 0, DEAKTIVERT = ? where UUID = ? ",
                dateProvider.getNow(),
                varselUUID);
    }

    private void opprettVarselForBruker(String aktorId) {
        jdbc.update("INSERT INTO VARSEL (AKTOR_ID, SENDT) VALUES (?, ?)",
                aktorId,
                dateProvider.getNow()
        );
    }
}
