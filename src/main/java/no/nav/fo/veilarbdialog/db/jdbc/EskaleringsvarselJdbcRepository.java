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
 * Spring Data JDBC repository for the {@code eskaleringsvarsel} table.
 */
public interface EskaleringsvarselJdbcRepository extends CrudRepository<EskaleringsvarselRow, Long> {

    @Query("insert into eskaleringsvarsel (aktor_id, gjeldende, opprettet_av, opprettet_dato, "
            + "tilhorende_dialog_id, tilhorende_minside_varsel, opprettet_begrunnelse) "
            + "values (:aktorId, :aktorId, :opprettetAv, :opprettetDato, :dialogId, :varselId, :begrunnelse) "
            + "returning id")
    Long insertAndReturnId(@Param("aktorId") String aktorId,
                           @Param("opprettetAv") String opprettetAv,
                           @Param("opprettetDato") LocalDateTime opprettetDato,
                           @Param("dialogId") long dialogId,
                           @Param("varselId") UUID varselId,
                           @Param("begrunnelse") String begrunnelse);

    @Modifying
    @Query("update eskaleringsvarsel set avsluttet_dato = :avsluttetDato, avsluttet_av = :avsluttetAv, "
            + "avsluttet_begrunnelse = :avsluttetBegrunnelse, gjeldende = null where id = :varselId")
    int stop(@Param("varselId") long varselId,
             @Param("avsluttetDato") LocalDateTime avsluttetDato,
             @Param("avsluttetAv") String avsluttetAv,
             @Param("avsluttetBegrunnelse") String avsluttetBegrunnelse);

    @Query("select * from eskaleringsvarsel where aktor_id = :aktorId and avsluttet_dato is null")
    Optional<EskaleringsvarselRow> findGjeldende(@Param("aktorId") String aktorId);

    @Query("select e.* from eskaleringsvarsel e inner join dialog on dialog.dialog_id = e.tilhorende_dialog_id "
            + "where dialog.oppfolgingsperiode_uuid = :oppfolgingsperiodeUuid and gjeldende is not null")
    Optional<EskaleringsvarselRow> findGjeldendeForPeriode(@Param("oppfolgingsperiodeUuid") String oppfolgingsperiodeUuid);

    @Modifying
    @Query("update eskaleringsvarsel set avsluttet_av = 'SYSTEM', avsluttet_dato = current_timestamp, "
            + "avsluttet_begrunnelse = 'Oppfolging avsluttet', gjeldende = null "
            + "where avsluttet_dato is null and exists "
            + "(select * from dialog where tilhorende_dialog_id = dialog_id "
            + "and oppfolgingsperiode_uuid = :oppfolgingsperiodeUuid)")
    int stopPeriode(@Param("oppfolgingsperiodeUuid") String oppfolgingsperiodeUuid);

    @Query("select * from eskaleringsvarsel where opprettet_dato < :tidspunkt "
            + "and gjeldende is not null and oversikten_melding_med_metadata_melding_key is null")
    List<EskaleringsvarselRow> findUsendteGjeldendeVarslerEldreEnn(@Param("tidspunkt") LocalDateTime tidspunkt);

    @Query("select * from eskaleringsvarsel where aktor_id = :aktorId order by opprettet_dato desc")
    List<EskaleringsvarselRow> findHistorikk(@Param("aktorId") String aktorId);

    @Modifying
    @Query("update eskaleringsvarsel set oversikten_melding_med_metadata_melding_key = :oversiktenSendingUuid "
            + "where id = :varselId")
    void knyttTilOversiktenMelding(@Param("varselId") long varselId,
                                   @Param("oversiktenSendingUuid") UUID oversiktenSendingUuid);

    @Query("select count(*) from eskaleringsvarsel where gjeldende is not null "
            + "and opprettet_dato < now() - interval '10 days'")
    int tellUtgaatteVarsler();
}
