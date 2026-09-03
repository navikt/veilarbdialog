package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Data JDBC entity for the {@code min_side_varsel} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("min_side_varsel")
public class MinSideVarselRow {

    @Id
    @Column("varsel_id")
    private UUID varselId;

    @Column("foedselsnummer")
    private String foedselsnummer;

    @Column("oppfolgingsperiode_id")
    private UUID oppfolgingsperiodeId;

    @Column("type")
    private String type;

    @Column("status")
    private String status;

    @Column("varsel_kvittering_status")
    private String varselKvitteringStatus;

    @Column("opprettet")
    private LocalDateTime opprettet;

    @Column("oppdatert")
    private LocalDateTime oppdatert;

    @Column("melding")
    private String melding;

    @Column("lenke")
    private String lenke;

    @Column("skal_batches")
    private Boolean skalBatches;
}
