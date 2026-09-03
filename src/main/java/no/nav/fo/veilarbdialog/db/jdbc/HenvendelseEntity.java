package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Spring Data JDBC entity for the {@code henvendelse} table.
 *
 * <p>The {@code viktig} flag is stored as {@code smallint} and mapped as
 * {@link Integer}; the owning DAO converts it to a boolean.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("henvendelse")
public class HenvendelseEntity {

    @Id
    @Column("henvendelse_id")
    private Long henvendelseId;

    @Column("dialog_id")
    private Long dialogId;

    @Column("sendt")
    private LocalDateTime sendt;

    @Column("tekst")
    private String tekst;

    @Column("kontorsperre_enhet_id")
    private String kontorsperreEnhetId;

    @Column("avsender_id")
    private String avsenderId;

    @Column("avsender_type")
    private String avsenderType;

    @Column("viktig")
    private Integer viktig;
}
