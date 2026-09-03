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
 * Spring Data JDBC repository for the {@code min_side_varsel} and
 * {@code min_side_varsel_dialog_mapping} tables.
 */
public interface MinSideVarselJdbcRepository extends CrudRepository<MinSideVarselRow, UUID> {

    @Query("select * from min_side_varsel where status = :status")
    List<MinSideVarselRow> findByStatus(@Param("status") String status);

    @Modifying
    @Query("update min_side_varsel set status = :status, oppdatert = current_timestamp where varsel_id = :varselId")
    int updateStatus(@Param("varselId") UUID varselId,
                     @Param("status") String status);

    @Modifying
    @Query("insert into min_side_varsel (varsel_id, foedselsnummer, oppfolgingsperiode_id, type, status, "
            + "skal_batches, melding, varsel_kvittering_status, lenke, opprettet) "
            + "values (:varselId, :fnr, :oppfolgingsperiodeId, :type, :status, :skalBatches, :melding, "
            + ":varselKvitteringStatus, :lenke, current_timestamp)")
    void insert(@Param("varselId") UUID varselId,
                @Param("fnr") String fnr,
                @Param("oppfolgingsperiodeId") UUID oppfolgingsperiodeId,
                @Param("type") String type,
                @Param("status") String status,
                @Param("skalBatches") boolean skalBatches,
                @Param("melding") String melding,
                @Param("varselKvitteringStatus") String varselKvitteringStatus,
                @Param("lenke") String lenke);

    @Query("select count(*) from min_side_varsel where varsel_id = :varselId")
    int countByVarselId(@Param("varselId") UUID varselId);

    @Modifying
    @Query("update min_side_varsel set oppdatert = current_timestamp, varsel_kvittering_status = :kvitteringStatus "
            + "where varsel_id = :varselId")
    void updateKvitteringStatus(@Param("varselId") UUID varselId,
                                @Param("kvitteringStatus") String kvitteringStatus);

    @Query("select min_side_varsel.varsel_id, min_side_varsel.status, min_side_varsel.opprettet, "
            + "min_side_varsel.varsel_kvittering_status, min_side_varsel.foedselsnummer, "
            + "min_side_varsel.oppfolgingsperiode_id, min_side_varsel.type, min_side_varsel.melding, "
            + "min_side_varsel.lenke, min_side_varsel.skal_batches, min_side_varsel.oppdatert "
            + "from eskaleringsvarsel join min_side_varsel "
            + "on min_side_varsel.varsel_id = eskaleringsvarsel.tilhorende_minside_varsel")
    List<MinSideVarselRow> findForForhandsvarsel();

    @Query("select min_side_varsel.varsel_id, min_side_varsel.status, min_side_varsel.opprettet, "
            + "min_side_varsel.varsel_kvittering_status, min_side_varsel.foedselsnummer, "
            + "min_side_varsel.oppfolgingsperiode_id, min_side_varsel.type, min_side_varsel.melding, "
            + "min_side_varsel.lenke, min_side_varsel.skal_batches, min_side_varsel.oppdatert "
            + "from min_side_varsel_dialog_mapping mapping join min_side_varsel "
            + "on min_side_varsel.varsel_id = mapping.varsel_id where dialog_id = :dialogId")
    List<MinSideVarselRow> findForDialog(@Param("dialogId") long dialogId);

    @Modifying
    @Query("insert into min_side_varsel_dialog_mapping (varsel_id, dialog_id) values (:varselId, :dialogId)")
    void insertMapping(@Param("varselId") UUID varselId,
                       @Param("dialogId") long dialogId);

    @Modifying
    @Query("update min_side_varsel set status = :tilStatus, oppdatert = current_timestamp "
            + "where oppfolgingsperiode_id = :oppfolgingsperiodeId and status = :fraStatus")
    void updateStatusForPeriode(@Param("oppfolgingsperiodeId") UUID oppfolgingsperiodeId,
                                @Param("fraStatus") String fraStatus,
                                @Param("tilStatus") String tilStatus);

    @Query("select count(*) from min_side_varsel where varsel_kvittering_status = 'IKKE_SATT' "
            + "and status = 'SENDT' and oppdatert < :date")
    int countUkvitterteForsoktSendt(@Param("date") LocalDateTime date);

    @Query("select * from min_side_varsel where varsel_id = :varselId")
    Optional<MinSideVarselRow> findVarsel(@Param("varselId") UUID varselId);
}
