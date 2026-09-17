package no.nav.fo.veilarbdialog.db.jdbc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC entity for the {@code dialog_egenskap} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("dialog_egenskap")
public class DialogEgenskapEntity {

    @Id
    @Column("rowid")
    private Long rowid;

    @Column("dialog_egenskap_type_kode")
    private String dialogEgenskapTypeKode;

    @Column("dialog_id")
    private Long dialogId;
}
