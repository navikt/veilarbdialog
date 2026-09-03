package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JDBC repository for the {@code dialog_egenskap} table.
 */
public interface DialogEgenskapRepository extends CrudRepository<DialogEgenskapEntity, Long> {

    @Query("select dialog_egenskap_type_kode from dialog_egenskap where dialog_id = :dialogId")
    List<String> findTyperByDialogId(@Param("dialogId") long dialogId);

    @Modifying
    @Query("insert into dialog_egenskap (dialog_id, dialog_egenskap_type_kode) "
            + "values (:dialogId, :dialogEgenskap)")
    void insert(@Param("dialogId") long dialogId,
                @Param("dialogEgenskap") String dialogEgenskap);
}
