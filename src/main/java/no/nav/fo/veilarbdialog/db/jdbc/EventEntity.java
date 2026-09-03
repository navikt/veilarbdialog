package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Spring Data JDBC entity for the {@code event} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("event")
public class EventEntity {

    @Id
    @Column("event_id")
    private Long eventId;

    @Column("dialogid")
    private Long dialogId;

    @Column("event")
    private String event;

    @Column("tidspunkt")
    private LocalDateTime tidspunkt;

    @Column("aktor_id")
    private String aktorId;

    @Column("aktivitet_id")
    private String aktivitetId;

    @Column("lagt_inn_av")
    private String lagtInnAv;
}
