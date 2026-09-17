package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JDBC repository for the {@code siste_oppfolgingsperiode} table.
 */
public interface SisteOppfolgingsperiodeRepository extends CrudRepository<SisteOppfolgingsperiodeEntity, String> {

    @Query("select * from siste_oppfolgingsperiode where aktorid = :aktorId")
    Optional<SisteOppfolgingsperiodeEntity> findByAktorid(@Param("aktorId") String aktorId);

    @Modifying
    @Query("insert into siste_oppfolgingsperiode (periode_uuid, aktorid, startdato, sluttdato) "
            + "values (:periodeUuid, :aktorId, :startdato, :sluttdato)")
    void insert(@Param("periodeUuid") String periodeUuid,
                @Param("aktorId") String aktorId,
                @Param("startdato") LocalDateTime startdato,
                @Param("sluttdato") LocalDateTime sluttdato);

    @Modifying
    @Query("update siste_oppfolgingsperiode set periode_uuid = :periodeUuid, startdato = :startdato, "
            + "sluttdato = :sluttdato where aktorid = :aktorId")
    int update(@Param("periodeUuid") String periodeUuid,
               @Param("aktorId") String aktorId,
               @Param("startdato") LocalDateTime startdato,
               @Param("sluttdato") LocalDateTime sluttdato);
}
