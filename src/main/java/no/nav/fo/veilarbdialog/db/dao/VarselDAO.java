package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VarselDAO {
    private final NamedParameterJdbcTemplate jdbc;

    public List<Long> hentDialogerMedUlesteMeldingerEtterSisteVarsel(long graceMillis, long maxAgeMillis) {
        final Date minimumAlder = new Date(System.currentTimeMillis() - graceMillis);
        final Date maksimumAlder = new Date(System.currentTimeMillis() - maxAgeMillis);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("avsenderType", AvsenderType.VEILEDER.name())
                .addValue("minimumAlder", minimumAlder)
                .addValue("maksimumAlder", maksimumAlder);
        String sql = """
                select DISTINCT d.DIALOG_ID
                    from DIALOG d
                        left join HENVENDELSE h on h.DIALOG_ID = d.DIALOG_ID
                        left join VARSEL v on v.AKTOR_ID = d.AKTOR_ID
                    where h.AVSENDER_TYPE = :avsenderType
                        and (d.LEST_AV_BRUKER_TID is null or h.SENDT > d.LEST_AV_BRUKER_TID)
                        and (v.SENDT is null or h.SENDT > v.SENDT)
                        and h.SENDT < :minimumAlder
                        and h.SENDT > :maksimumAlder
                """;
        return jdbc.queryForList(sql,
                params,
                Long.class);
    }

    public List<String> hentAktorerMedUlesteMeldingerEtterSisteVarsel(long graceMillis) {
        final Date grense = new Date(System.currentTimeMillis() - graceMillis);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("avsenderType", AvsenderType.VEILEDER.name())
                .addValue("enStundSiden", grense);
        String sql = """
                select d.AKTOR_ID
                    from DIALOG d
                        left join HENVENDELSE h on h.DIALOG_ID = d.DIALOG_ID
                        left join VARSEL v on v.AKTOR_ID = d.AKTOR_ID
                where h.AVSENDER_TYPE = :avsenderType
                    and (d.LEST_AV_BRUKER_TID is null or h.SENDT > d.LEST_AV_BRUKER_TID)
                    and (v.SENDT is null or h.SENDT > v.SENDT)
                    and h.SENDT < :enStundSiden
                group by d.AKTOR_ID
                """;
        return jdbc.queryForList(sql, params, String.class);
    }

    public void oppdaterSisteVarselForBruker(String aktorId) {
        var param = new MapSqlParameterSource("aktorId", aktorId);
        var rowsUpdated = jdbc.update("update VARSEL set SENDT = CURRENT_TIMESTAMP where AKTOR_ID = :aktorId" , param);
        if (rowsUpdated == 0) {
            opprettVarselForBruker(aktorId);
        }
    }

    private void opprettVarselForBruker(String aktorId) {
        var param = new MapSqlParameterSource("aktorId", aktorId);
        jdbc.update("INSERT INTO VARSEL (AKTOR_ID, SENDT) VALUES (:aktorId, CURRENT_TIMESTAMP)", param);
    }
}
