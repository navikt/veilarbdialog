package no.nav.fo.veilarbdialog.kafka;

import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;


public class KafkaDAO {
    private String TABLE_NAME = "FEILEDE_KAFKA_MELDINGER";
    private final JdbcTemplate jdbc;

    public KafkaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertFeiletMelding(KafkaDialogMelding melding) {
        SqlUtils
                .insert(jdbc, TABLE_NAME)
                .value("AKTOR_ID", melding.getAktorId())
                .value("TIDSPUNKT_ELDSTE_VENTENDE", (Optional.ofNullable(melding.getTidspunktEldsteVentende()).map(Timestamp::valueOf)).orElse(null))
                .value("TIDSPUNKT_ELDSTE_UBEHANDLEDE", (Optional.ofNullable(melding.getTidspunktEldsteUbehandlede()).map(Timestamp::valueOf)).orElse(null))
                .value("OPPRETTET_TIDSPUNKT", Timestamp.valueOf(melding.getTidspunktOpprettet()))
                .execute();
    }

    public int slettFeiletMelding(KafkaDialogMelding melding) {
        return SqlUtils
                .delete(jdbc, TABLE_NAME)
                .where(WhereClause.equals("AKTOR_ID", melding.getAktorId())
                        .and(WhereClause.equals("OPPRETTET_TIDSPUNKT", melding.getTidspunktOpprettet())))
                .execute();
    }

    public List<KafkaDialogMelding> hentAlleFeilendeMeldinger() {
        return  SqlUtils
                .select(jdbc, TABLE_NAME, KafkaDAO::mapRSTilKafkaMelding)
                .column("*")
                .executeToList();
    }

    private static KafkaDialogMelding mapRSTilKafkaMelding(ResultSet rs) throws SQLException {
        return KafkaDialogMelding.builder()
                .aktorId(rs.getString("AKTOR_ID"))
                .tidspunktEldsteUbehandlede(Optional.ofNullable(rs.getTimestamp("TIDSPUNKT_ELDSTE_UBEHANDLEDE")).map(v -> v.toLocalDateTime()).orElse(null))
                .tidspunktEldsteVentende(Optional.ofNullable(rs.getTimestamp("TIDSPUNKT_ELDSTE_VENTENDE")).map(v -> v.toLocalDateTime()).orElse(null))
                .tidspunktOpprettet(rs.getTimestamp("OPPRETTET_TIDSPUNKT").toLocalDateTime())
                .build();
    }
}
