package no.nav.fo.veilarbdialog.db.dao;

import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class KafkaDAO {
    private String TABLE_NAME = "FEILEDE_KAFKA_AKTOR_ID";
    private final JdbcTemplate jdbc;

    @Inject
    public KafkaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertFeiletAktorId(String aktorId) {
        SqlUtils
                .insert(jdbc, TABLE_NAME)
                .value("AKTOR_ID", aktorId)
                .execute();
    }

    public int slettFeiletAktorId(String aktorId) {
        return SqlUtils
                .delete(jdbc, TABLE_NAME)
                .where(WhereClause.equals("AKTOR_ID", aktorId))
                .execute();
    }

    public List<String> hentAlleFeilendeAktorId() {
        return  SqlUtils
                .select(jdbc, TABLE_NAME, rs -> rs.getString("AKTOR_ID"))
                .column("*")
                .executeToList();
    }

}
