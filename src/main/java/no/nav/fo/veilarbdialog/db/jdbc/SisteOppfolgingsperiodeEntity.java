package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Spring Data JDBC entity for the {@code siste_oppfolgingsperiode} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("siste_oppfolgingsperiode")
public class SisteOppfolgingsperiodeEntity {

    @Id
    @Column("aktorid")
    private String aktorid;

    @Column("periode_uuid")
    private String periodeUuid;

    @Column("startdato")
    private LocalDateTime startdato;

    @Column("sluttdato")
    private LocalDateTime sluttdato;
}
