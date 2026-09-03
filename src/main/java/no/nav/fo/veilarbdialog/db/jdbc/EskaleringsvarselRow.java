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
 * Spring Data JDBC entity for the {@code eskaleringsvarsel} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("eskaleringsvarsel")
public class EskaleringsvarselRow {

    @Id
    @Column("id")
    private Long id;

    @Column("aktor_id")
    private String aktorId;

    @Column("opprettet_av")
    private String opprettetAv;

    @Column("opprettet_dato")
    private LocalDateTime opprettetDato;

    @Column("tilhorende_dialog_id")
    private Long tilhorendeDialogId;

    @Column("tilhorende_minside_varsel")
    private UUID tilhorendeMinsideVarsel;

    @Column("opprettet_begrunnelse")
    private String opprettetBegrunnelse;

    @Column("avsluttet_dato")
    private LocalDateTime avsluttetDato;

    @Column("avsluttet_av")
    private String avsluttetAv;

    @Column("avsluttet_begrunnelse")
    private String avsluttetBegrunnelse;

    @Column("oversikten_melding_med_metadata_melding_key")
    private UUID oversiktenMeldingKey;

    @Column("gjeldende")
    private String gjeldende;
}
