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
 * Spring Data JDBC entity for the {@code oversikten_melding_med_metadata} table.
 *
 * <p>Enum columns use Postgres enum types and the {@code melding} column is
 * {@code json}; all are mapped as {@link String} and (de)serialised by the
 * owning repository/DAO, which keeps the required {@code ::TYPE} casts in SQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("oversikten_melding_med_metadata")
public class OversiktenMeldingRow {

    @Id
    @Column("id")
    private Long id;

    @Column("melding_key")
    private UUID meldingKey;

    @Column("fnr")
    private String fnr;

    @Column("opprettet")
    private LocalDateTime opprettet;

    @Column("tidspunkt_sendt")
    private LocalDateTime tidspunktSendt;

    @Column("utsending_status")
    private String utsendingStatus;

    @Column("melding")
    private String meldingSomJson;

    @Column("kategori")
    private String kategori;

    @Column("operasjon")
    private String operasjon;
}
