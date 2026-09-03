package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JDBC repository for the {@code oversikten_melding_med_metadata} table.
 */
public interface OversiktenJdbcRepository extends CrudRepository<OversiktenMeldingRow, Long> {

    @Query("insert into oversikten_melding_med_metadata "
            + "(fnr, opprettet, utsending_status, melding, kategori, melding_key, operasjon) "
            + "values (:fnr, :opprettet, :utsendingStatus::oversikten_utsending_status, :melding::json, "
            + ":kategori::oversikten_kategori, :meldingKey, :operasjon::oversikten_operasjon) returning id")
    Long insertAndReturnId(@Param("fnr") String fnr,
                           @Param("opprettet") LocalDateTime opprettet,
                           @Param("utsendingStatus") String utsendingStatus,
                           @Param("melding") String melding,
                           @Param("kategori") String kategori,
                           @Param("meldingKey") UUID meldingKey,
                           @Param("operasjon") String operasjon);

    // NB: melding is a json column and must be cast to text, otherwise the
    // driver returns a PGobject which Spring Data JDBC cannot convert.
    String OVERSIKTEN_KOLONNER = "id, melding_key, fnr, opprettet, tidspunkt_sendt, utsending_status, "
            + "melding::text as melding, kategori, operasjon";

    @Query("select " + OVERSIKTEN_KOLONNER + " from oversikten_melding_med_metadata "
            + "where utsending_status = 'SKAL_SENDES'")
    List<OversiktenMeldingRow> findAlleSomSkalSendes();

    @Query("select " + OVERSIKTEN_KOLONNER + " from oversikten_melding_med_metadata "
            + "where melding_key = :meldingKey and melding->>'operasjon' = :operasjon")
    List<OversiktenMeldingRow> findByMeldingKeyAndOperasjon(@Param("meldingKey") UUID meldingKey,
                                                           @Param("operasjon") String operasjon);

    @Query("select " + OVERSIKTEN_KOLONNER + " from oversikten_melding_med_metadata where id = :id")
    Optional<OversiktenMeldingRow> findRowById(@Param("id") long id);

    @Modifying
    @Query("update oversikten_melding_med_metadata set utsending_status = 'SENDT', tidspunkt_sendt = now() "
            + "where id = :id")
    void markerSomSendt(@Param("id") long id);
}
