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
        return jdbc.queryForList("select d.aktor_id " +
                        "from DIALOG d " +
                        "left join HENVENDELSE h on h.dialog_id = d.dialog_id " +
                        "left join VARSEL v on v.aktor_id = d.aktor_id " +
                        "where h.avsender_type = ? " +
                        "and (d.lest_av_bruker_tid is null or h.sendt > d.lest_av_bruker_tid) " +
                        "and (v.sendt is null or h.sendt > v.sendt) " +
                        "and h.sendt < ? " +
                        "group by d.aktor_id",
                new Object[]{AvsenderType.VEILEDER.name(), grense},
                String.class
        );
    }

    public void oppdaterSisteVarselForBruker(String aktorId) {
        final int rowsUpdated = jdbc.update("update varsel set sendt = ? where aktor_id = ?", dateProvider.getNow(), aktorId);
        if (rowsUpdated == 0) {
            opprettVarselForBruker(aktorId);
        }
    }

    public void setVarselUUIDForParagraf8Dialoger(String aktorId, String varselUUID) {
        jdbc.update("update DIALOG " +
                        "set paragraf8_varsel_uuid = ? " +
                        "where paragraf8_varsel_uuid is null " +
                        "and ulestParagraf8Varsel = 1 " +
                        "and aktor_id = ?",
                varselUUID,
                aktorId);
    }

    public boolean harUlesteUvarsledeParagraf8Henvendelser(String aktorId) {
        final int unread = Optional.ofNullable(
                jdbc.queryForObject("select count(aktor_id) " +
                                "from DIALOG " +
                                "where ulestParagraf8Varsel = 1 " +
                                "and paragraf8_varsel_uuid is null " +
                                "and aktor_id = ?",
                        new Object[]{aktorId},
                        Integer.class))
                .orElse(0);
        return unread > 0;
    }

    public int hentAntallAktiveDialogerForVarsel(String paragraf8VarselUUID) {
        return Optional.ofNullable(
                jdbc.queryForObject("select count(*) as antall " +
                                "from DIALOG " +
                                "where paragraf8_varsel_uuid = ? " +
                                "and ULESTPARAGRAF8VARSEL = 1",
                        new Object[]{paragraf8VarselUUID},
                        Integer.class))
                .orElse(0);
    }

    public void insertParagraf8Varsel(String aktorid, String varselUuid) {
        jdbc.update("insert into PARAGRAF8VARSEL (uuid, aktorid, sendt) values (?, ?, " + dateProvider.getNow() + ")",
                varselUuid,
                aktorid);
    }

    public void revarslingSkalAvsluttes(String paragraf8VarselUUID) {
        jdbc.update("update PARAGRAF8VARSEL set skalStoppes = 1 where UUID = ?",
                paragraf8VarselUUID);
    }

    public List<String> hentRevarslerSomSkalStoppes() {
        return jdbc.queryForList("select UUID from PARAGRAF8VARSEL where skalStoppes = 1", String.class);
    }

    public void markerSomStoppet(String varselUUID) {
        jdbc.update("update PARAGRAF8VARSEL set skalStoppes = 0, deaktivert = ? where UUID = ? ",
                dateProvider.getNow(),
                varselUUID);
    }

    private void opprettVarselForBruker(String aktorId) {
        jdbc.update("INSERT INTO VARSEL (aktor_id, sendt) VALUES (?, ?)",
                aktorId,
                dateProvider.getNow()
        );
    }
}
