package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JDBC repository for the {@code henvendelse} table.
 */
public interface HenvendelseRepository extends CrudRepository<HenvendelseEntity, Long> {

    @Query("select nextval('henvendelse_id_seq')")
    Long nextHenvendelseId();

    @Query("select * from henvendelse where dialog_id = :dialogId")
    List<HenvendelseEntity> findByDialogId(@Param("dialogId") long dialogId);

    @Modifying
    @Query("insert into henvendelse (henvendelse_id, dialog_id, sendt, tekst, kontorsperre_enhet_id, "
            + "avsender_id, avsender_type, viktig) "
            + "values (:henvendelseId, :dialogId, :sendt, :tekst, :kontorsperreEnhetId, "
            + ":avsenderId, :avsenderType, :viktig)")
    void insert(@Param("henvendelseId") long henvendelseId,
                @Param("dialogId") long dialogId,
                @Param("sendt") LocalDateTime sendt,
                @Param("tekst") String tekst,
                @Param("kontorsperreEnhetId") String kontorsperreEnhetId,
                @Param("avsenderId") String avsenderId,
                @Param("avsenderType") String avsenderType,
                @Param("viktig") Integer viktig);

    @Modifying
    @Query("update henvendelse set tekst = '- Det var skrevet noe feil, og det er nå slettet. -' "
            + "where henvendelse_id = :henvendelseId")
    int kasserHenvendelse(@Param("henvendelseId") long henvendelseId);
}
