package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JDBC repository for the {@code kladd} table.
 */
public interface KladdRepository extends CrudRepository<KladdEntity, Long> {

    @Query("select nextval('kladd_id_seq')")
    Long nextKladdSeq();

    @Modifying
    @Query("insert into kladd (aktor_id, dialog_id, aktivitet_id, overskrift, tekst, lagt_inn_av, oppdatert, unique_seq) "
            + "values (:aktorId, :dialogId, :aktivitetId, :overskrift, :tekst, :lagtInnAv, :oppdatert, :kladdSeq)")
    void insert(@Param("aktorId") String aktorId,
                @Param("dialogId") Long dialogId,
                @Param("aktivitetId") String aktivitetId,
                @Param("overskrift") String overskrift,
                @Param("tekst") String tekst,
                @Param("lagtInnAv") String lagtInnAv,
                @Param("oppdatert") LocalDateTime oppdatert,
                @Param("kladdSeq") long kladdSeq);

    @Modifying
    @Query("delete from kladd where unique_seq < :kladdSeq and lagt_inn_av = :lagtInnAv and aktor_id = :aktorId "
            + "and (dialog_id = :dialogId or (dialog_id is null and :dialogId is null)) "
            + "and (dialog_id is not null or (aktivitet_id = :aktivitetId "
            + "or (aktivitet_id is null and :aktivitetId is null)))")
    void deleteEldreEnnUniqueSeq(@Param("kladdSeq") long kladdSeq,
                                 @Param("lagtInnAv") String lagtInnAv,
                                 @Param("aktorId") String aktorId,
                                 @Param("dialogId") Long dialogId,
                                 @Param("aktivitetId") String aktivitetId);

    @Query("select * from kladd where aktor_id = :aktorId and lagt_inn_av = :lagtInnAv order by unique_seq")
    List<KladdEntity> findByAktorIdAndLagtInnAv(@Param("aktorId") String aktorId,
                                                @Param("lagtInnAv") String lagtInnAv);

    @Modifying
    @Query("delete from kladd where oppdatert <= :olderThanThis")
    void deleteGamlereEnn(@Param("olderThanThis") LocalDateTime olderThanThis);

    @Modifying
    @Query("delete from kladd where aktor_id = :aktorId and lagt_inn_av = :lagtInnAv "
            + "and (dialog_id = :dialogId or (dialog_id is null and :dialogId is null)) "
            + "and (dialog_id is not null or (aktivitet_id = :aktivitetId "
            + "or (aktivitet_id is null and :aktivitetId is null)))")
    void deleteKladd(@Param("aktorId") String aktorId,
                     @Param("lagtInnAv") String lagtInnAv,
                     @Param("dialogId") Long dialogId,
                     @Param("aktivitetId") String aktivitetId);
}
