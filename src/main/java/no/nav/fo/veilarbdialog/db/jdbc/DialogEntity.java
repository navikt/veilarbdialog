package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Spring Data JDBC entity for the {@code dialog} table.
 *
 * <p>Flag columns ({@code historisk}, {@code ulestparagraf8varsel}) are stored as
 * {@code smallint} (0/1) and mapped as {@link Integer}; the owning DAO converts
 * them to booleans. UUID-valued columns are stored as {@code varchar} and mapped
 * as {@link String}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("dialog")
public class DialogEntity {

    @Id
    @Column("dialog_id")
    private Long dialogId;

    @Column("aktor_id")
    private String aktorId;

    @Column("overskrift")
    private String overskrift;

    @Column("lest_av_bruker_tid")
    private LocalDateTime lestAvBrukerTid;

    @Column("lest_av_veileder_tid")
    private LocalDateTime lestAvVeilederTid;

    @Column("aktivitet_id")
    private String aktivitetId;

    @Column("arena_id")
    private String arenaId;

    @Column("historisk")
    private Integer historisk;

    @Column("opprettet_dato")
    private LocalDateTime opprettetDato;

    @Column("eldste_uleste_for_bruker")
    private LocalDateTime eldsteUlesteForBruker;

    @Column("eldste_uleste_for_veileder")
    private LocalDateTime eldsteUlesteForVeileder;

    @Column("venter_pa_nav_siden")
    private LocalDateTime venterPaNavSiden;

    @Column("venter_pa_svar_fra_bruker")
    private LocalDateTime venterPaSvarFraBruker;

    @Column("oppdatert")
    private LocalDateTime oppdatert;

    @Column("kontorsperre_enhet_id")
    private String kontorsperreEnhetId;

    @Column("ulestparagraf8varsel")
    private Integer ulestparagraf8varsel;

    @Column("paragraf8_varsel_uuid")
    private String paragraf8VarselUuid;

    @Column("oppfolgingsperiode_uuid")
    private String oppfolgingsperiodeUuid;
}
