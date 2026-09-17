package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JDBC repository for the {@code outbox} table.
 */
public interface OutboxJdbcRepository extends CrudRepository<OutboxRow, Long> {

    @Modifying
    @Query("insert into outbox (topic, key, payload) values (:topic, :key, :payload)")
    void insert(@Param("topic") String topic,
                @Param("key") String key,
                @Param("payload") String payload);

    @Query("select id, topic, key, payload from outbox order by opprettet for update skip locked")
    List<OutboxRow> findUsendte();

    @Query("select id, topic, key, payload from outbox where key = :key and topic = :topic "
            + "order by opprettet for update skip locked")
    List<OutboxRow> findUsendteForKeyAndTopic(@Param("key") String key,
                                              @Param("topic") String topic);
}
