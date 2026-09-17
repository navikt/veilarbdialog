package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JDBC repository for the {@code event} table.
 */
public interface EventRepository extends CrudRepository<EventEntity, Long> {

    @Query("select nextval('event_id_seq')")
    Long nextEventId();

    @Modifying
    @Query("insert into event (event_id, dialogid, event, tidspunkt, aktor_id, aktivitet_id, lagt_inn_av) "
            + "values (:eventId, :dialogId, :event, current_timestamp, :aktorId, :aktivitetId, :lagtInnAv)")
    void insert(@Param("eventId") long eventId,
                @Param("dialogId") long dialogId,
                @Param("event") String event,
                @Param("aktorId") String aktorId,
                @Param("aktivitetId") String aktivitetId,
                @Param("lagtInnAv") String lagtInnAv);

    @Query("select tidspunkt from event where aktor_id = :aktorId and lagt_inn_av != :bruker "
            + "order by event_id desc fetch first 1 rows only")
    Optional<LocalDateTime> findSisteEndringSomIkkeErDine(@Param("aktorId") String aktorId,
                                                         @Param("bruker") String bruker);
}
