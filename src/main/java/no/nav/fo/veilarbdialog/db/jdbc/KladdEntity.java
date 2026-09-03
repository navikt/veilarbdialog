package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Spring Data JDBC entity for the {@code kladd} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("kladd")
public class KladdEntity {

    @Id
    @Column("rowid")
    private Long rowid;

    @Column("aktor_id")
    private String aktorId;

    @Column("dialog_id")
    private Long dialogId;

    @Column("aktivitet_id")
    private String aktivitetId;

    @Column("overskrift")
    private String overskrift;

    @Column("tekst")
    private String tekst;

    @Column("lagt_inn_av")
    private String lagtInnAv;

    @Column("oppdatert")
    private LocalDateTime oppdatert;

    @Column("opprettet")
    private LocalDateTime opprettet;

    @Column("unique_seq")
    private Long uniqueSeq;
}
