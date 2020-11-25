package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KafkaDAO {

    private final JdbcTemplate jdbc;

    public void insertFeiletAktorId(String aktorId) {
        jdbc.update("insert into FEILEDE_KAFKA_AKTOR_ID (AKTOR_ID) values (?)",
                aktorId);
    }

    public void slettFeiletAktorId(String aktorId) {
        jdbc.update("delete from FEILEDE_KAFKA_AKTOR_ID where AKTOR_ID = ?",
                aktorId);
    }

    public List<String> hentAlleFeilendeAktorId() {
        return jdbc.queryForList("select AKTOR_ID from FEILEDE_KAFKA_AKTOR_ID",
                String.class);
    }

}
