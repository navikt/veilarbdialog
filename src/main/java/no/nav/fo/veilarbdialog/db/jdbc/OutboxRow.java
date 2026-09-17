package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC entity for the {@code outbox} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("outbox")
public class OutboxRow {

    @Id
    @Column("id")
    private Long id;

    @Column("topic")
    private String topic;

    @Column("key")
    private String messageKey;

    @Column("payload")
    private String payload;
}
