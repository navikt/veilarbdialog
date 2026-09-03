package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JDBC repository for the {@code varsel} table, including the
 * cross-table query for actors with unread messages.
 */
public interface VarselRepository extends CrudRepository<VarselEntity, String> {

    @Query("select d.aktor_id from dialog d "
            + "left join henvendelse h on h.dialog_id = d.dialog_id "
            + "left join varsel v on v.aktor_id = d.aktor_id "
            + "where h.avsender_type = :avsenderType "
            + "and (d.lest_av_bruker_tid is null or h.sendt > d.lest_av_bruker_tid) "
            + "and (v.sendt is null or h.sendt > v.sendt) "
            + "and h.sendt < :enStundSiden "
            + "group by d.aktor_id")
    List<String> findAktorerMedUlesteMeldingerEtterSisteVarsel(@Param("avsenderType") String avsenderType,
                                                              @Param("enStundSiden") LocalDateTime enStundSiden);

    @Modifying
    @Query("update varsel set sendt = current_timestamp where aktor_id = :aktorId")
    int oppdaterSisteVarsel(@Param("aktorId") String aktorId);

    @Modifying
    @Query("insert into varsel (aktor_id, sendt) values (:aktorId, current_timestamp)")
    void insert(@Param("aktorId") String aktorId);
}
