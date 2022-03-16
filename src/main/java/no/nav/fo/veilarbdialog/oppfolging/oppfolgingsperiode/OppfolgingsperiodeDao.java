package no.nav.fo.veilarbdialog.oppfolging.oppfolgingsperiode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;


@Repository
@Slf4j
@RequiredArgsConstructor
public class OppfolgingsperiodeDao {
    private final NamedParameterJdbcTemplate template;

    public long oppdaterAktiviteterForPeriode(AktorId aktorId, ZonedDateTime startDato, ZonedDateTime sluttDato, UUID uuid) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get())
                .addValue("startDato", startDato)
                .addValue("sluttDato", sluttDato)
                .addValue("oppfolgingsperiodeId", uuid.toString());

        if (sluttDato != null) {
            return template.update("""
                    UPDATE DIALOG SET OPPFOLGINGSPERIODE_UUID = :oppfolgingsperiodeId
                    WHERE AKTOR_ID = :aktorId
                    AND OPPRETTET_DATO BETWEEN :startDato AND :sluttDato
                    AND OPPFOLGINGSPERIODE_UUID IS NULL
                    """, params);
        } else {
            // aktiv (siste) periode
            return template.update("""
                    UPDATE DIALOG SET OPPFOLGINGSPERIODE_UUID = :oppfolgingsperiodeId
                    WHERE AKTOR_ID = :aktorId
                    AND OPPRETTET_DATO >= :startDato
                    AND OPPFOLGINGSPERIODE_UUID IS NULL
                    """, params);
        }
    }

    public AktorId hentEnBrukerUtenOppfolgingsperiode() {
        String aktorId;
        try {
            aktorId = template.getJdbcTemplate().queryForObject("""
                    SELECT AKTOR_ID from DIALOG
                    where OPPFOLGINGSPERIODE_UUID is null
                    fetch next 1 row ONLY
                    """, String.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

        return new AktorId(aktorId);
    }

    public void setUkjentAktorId(AktorId aktorId) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get());

        template.update("""
                    update DIALOG
                    set OPPFOLGINGSPERIODE_UUID = 'ukjent aktorId'
                    where AKTOR_ID = :aktorId
                    and OPPFOLGINGSPERIODE_UUID is null
                """, source);
    }

    public void setOppfolgingsperiodeTilUkjentForGamleAktiviteterUtenOppfolgingsperiode(AktorId aktorId) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get());

        int antallOppdatert = template.update("""
                    update DIALOG
                    set OPPFOLGINGSPERIODE_UUID = 'ukjent_oppfolgingsperiode'
                    where AKTOR_ID = :aktorId
                    and OPPFOLGINGSPERIODE_UUID is null
                """, source);

        if (antallOppdatert != 0) {
            log.warn("Oppdaterete aktivitere med ukjent oppfolgingsperiodeId for aktorid {} antall: {}", aktorId.get(), antallOppdatert);
        }
    }

    public void setIngenPerioder(AktorId aktorId) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get());
        template.update("""
                update DIALOG set OPPFOLGINGSPERIODE_UUID = 'ingenPeriode' where AKTOR_ID = :aktorId
                """, source);
    }

    public void setAlleTilPeriode(AktorId aktorId, UUID oppfolingsperiode) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get())
                .addValue("periode", oppfolingsperiode.toString());

        template.update("""
                update DIALOG set OPPFOLGINGSPERIODE_UUID = :periode where AKTOR_ID = :aktorId
                """, source);
    }
}
