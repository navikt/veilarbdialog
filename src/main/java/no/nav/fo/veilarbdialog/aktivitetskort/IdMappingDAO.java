package no.nav.fo.veilarbdialog.aktivitetskort;


import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.Arenaid;
import no.nav.fo.veilarbdialog.domain.TekniskId;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdMappingDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public int migrerArenaDialogerTilTekniskId(Arenaid arenaId, TekniskId tekniskId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitetId", tekniskId.getId())
                .addValue("arenaId", arenaId.getId());

        return jdbcTemplate.update("""
                UPDATE DIALOG SET AKTIVITET_ID = :aktivitetId, ARENA_ID = :arenaId 
                WHERE ARENA_ID = :arenaId OR (AKTIVITET_ID = :arenaId AND ARENA_ID is null)
                """, params);
    }
}

