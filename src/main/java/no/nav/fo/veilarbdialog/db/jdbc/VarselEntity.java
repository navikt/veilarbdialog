package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Spring Data JDBC entity for the {@code varsel} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("varsel")
public class VarselEntity {

    @Id
    @Column("aktor_id")
    private String aktorId;

    @Column("sendt")
    private LocalDateTime sendt;
}
