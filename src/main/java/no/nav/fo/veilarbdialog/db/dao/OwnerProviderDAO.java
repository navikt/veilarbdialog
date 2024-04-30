package no.nav.fo.veilarbdialog.db.dao;

import lombok.AllArgsConstructor;
import no.nav.common.types.identer.AktorId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@AllArgsConstructor
@Repository
public class OwnerProviderDAO {
    NamedParameterJdbcTemplate template;

    public Optional<AktorId> getDialogOwner(long dialogId) {
        var query = """
            SELECT AKTOR_ID FROM DIALOG WHERE DIALOG_ID = :dialogId
        """;
        var params = new MapSqlParameterSource()
            .addValue("dialogId", dialogId);
        return template.query(query, params, rowMapper)
            .stream().findFirst();
    }

    private static final RowMapper<AktorId> rowMapper = (rs, rowNum) -> AktorId.of(
        rs.getString("AKTOR_ID")
    );
}
