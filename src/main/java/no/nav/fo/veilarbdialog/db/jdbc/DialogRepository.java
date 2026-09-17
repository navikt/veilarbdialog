package no.nav.fo.veilarbdialog.db.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JDBC repository for the {@code dialog} table.
 *
 * <p>Replaces the {@code DIALOG} queries previously spread across
 * {@code DialogDAO}, {@code StatusDAO}, {@code IdMappingDAO} and
 * {@code OwnerProviderDAO}, which used {@code NamedParameterJdbcTemplate} /
 * {@code JdbcTemplate} directly.
 */
public interface DialogRepository extends CrudRepository<DialogEntity, Long> {

    @Query("select nextval('dialog_id_seq')")
    Long nextDialogId();

    @Query("select * from dialog where aktor_id = :aktorId")
    List<DialogEntity> findByAktorId(@Param("aktorId") String aktorId);

    @Query("select * from dialog where oppfolgingsperiode_uuid = :oppfolgingsperiodeUuid")
    List<DialogEntity> findByOppfolgingsperiodeUuid(@Param("oppfolgingsperiodeUuid") String oppfolgingsperiodeUuid);

    @Query("select * from dialog where aktor_id = :aktorId and historisk = 0 "
            + "and opprettet_dato < :avsluttetDato and kontorsperre_enhet_id is not null")
    List<DialogEntity> findKontorsperredeSomSkalAvsluttes(@Param("aktorId") String aktorId,
                                                         @Param("avsluttetDato") LocalDateTime avsluttetDato);

    @Query("select * from dialog where aktor_id = :aktorId and historisk = 0 "
            + "and oppfolgingsperiode_uuid = :oppfolgingsperiodeUuid")
    List<DialogEntity> findSomSkalAvsluttes(@Param("aktorId") String aktorId,
                                            @Param("oppfolgingsperiodeUuid") String oppfolgingsperiodeUuid);

    @Query("select * from dialog where dialog_id = :dialogId and aktor_id = :aktorId")
    Optional<DialogEntity> findByDialogIdAndAktorId(@Param("dialogId") long dialogId,
                                                    @Param("aktorId") String aktorId);

    @Query("select d.* from dialog d left join henvendelse h on h.dialog_id = d.dialog_id "
            + "where h.henvendelse_id = :henvendelseId")
    Optional<DialogEntity> findByHenvendelseId(@Param("henvendelseId") long henvendelseId);

    @Query("select * from dialog where aktivitet_id = :aktivitetId and aktor_id = :aktorId")
    List<DialogEntity> findByAktivitetIdAndAktorId(@Param("aktivitetId") String aktivitetId,
                                                  @Param("aktorId") String aktorId);

    @Query("select * from dialog where arena_id = :arenaId and aktor_id = :aktorId")
    List<DialogEntity> findByArenaIdAndAktorId(@Param("arenaId") String arenaId,
                                               @Param("aktorId") String aktorId);

    @Query("select distinct aktor_id from dialog where historisk = 0")
    List<String> findAktiveAktorIder();

    @Query("select aktor_id from dialog where dialog_id = :dialogId")
    List<String> findAktorIdByDialogId(@Param("dialogId") long dialogId);

    @Modifying
    @Query("insert into dialog (dialog_id, aktor_id, opprettet_dato, aktivitet_id, arena_id, overskrift, "
            + "historisk, kontorsperre_enhet_id, oppdatert, oppfolgingsperiode_uuid) "
            + "values (:dialogId, :aktorId, :opprettetDato, :aktivitetId, :arenaId, :overskrift, "
            + ":historisk, :kontorsperreEnhetId, :oppdatert, :oppfolgingsperiodeUuid)")
    void insert(@Param("dialogId") long dialogId,
                @Param("aktorId") String aktorId,
                @Param("opprettetDato") LocalDateTime opprettetDato,
                @Param("aktivitetId") String aktivitetId,
                @Param("arenaId") String arenaId,
                @Param("overskrift") String overskrift,
                @Param("historisk") int historisk,
                @Param("kontorsperreEnhetId") String kontorsperreEnhetId,
                @Param("oppdatert") LocalDateTime oppdatert,
                @Param("oppfolgingsperiodeUuid") String oppfolgingsperiodeUuid);

    @Modifying
    @Query("update dialog set overskrift = '- Det var skrevet noe feil, og det er nå slettet. -' "
            + "where dialog_id = :dialogId")
    int kasserDialog(@Param("dialogId") long dialogId);

    @Modifying
    @Query("update dialog set aktivitet_id = :aktivitetId, arena_id = :arenaId "
            + "where arena_id = :arenaId or (aktivitet_id = :arenaId and arena_id is null)")
    int migrerArenaTilTekniskId(@Param("aktivitetId") String aktivitetId,
                                @Param("arenaId") String arenaId);

    @Modifying
    @Query("update dialog set eldste_uleste_for_veileder = :eldsteUleste, lest_av_veileder_tid = :lestTidspunkt, "
            + "oppdatert = :lestTidspunkt where dialog_id = :dialogId")
    void markerSomLestAvVeileder(@Param("dialogId") long dialogId,
                                 @Param("eldsteUleste") LocalDateTime eldsteUleste,
                                 @Param("lestTidspunkt") LocalDateTime lestTidspunkt);

    @Modifying
    @Query("update dialog set ulestparagraf8varsel = 0, eldste_uleste_for_bruker = null, "
            + "lest_av_bruker_tid = current_timestamp, oppdatert = current_timestamp where dialog_id = :dialogId")
    void markerSomLestAvBruker(@Param("dialogId") long dialogId);

    @Modifying
    @Query("update dialog set venter_pa_nav_siden = current_timestamp, oppdatert = current_timestamp "
            + "where dialog_id = :dialogId")
    void setVenterPaNavTilNaa(@Param("dialogId") long dialogId);

    @Modifying
    @Query("update dialog set venter_pa_svar_fra_bruker = current_timestamp, oppdatert = current_timestamp "
            + "where dialog_id = :dialogId")
    void setVenterPaSvarFraBrukerTilNaa(@Param("dialogId") long dialogId);

    @Modifying
    @Query("update dialog set venter_pa_nav_siden = null, oppdatert = current_timestamp "
            + "where dialog_id = :dialogId")
    void setVenterPaNavTilNull(@Param("dialogId") long dialogId);

    @Modifying
    @Query("update dialog set venter_pa_svar_fra_bruker = null, oppdatert = current_timestamp "
            + "where dialog_id = :dialogId")
    void setVenterPaSvarFraBrukerTilNull(@Param("dialogId") long dialogId);

    @Modifying
    @Query("update dialog set eldste_uleste_for_bruker = :dato, oppdatert = current_timestamp "
            + "where dialog_id = :dialogId")
    void setEldsteUlesteForBruker(@Param("dialogId") long dialogId,
                                  @Param("dato") LocalDateTime dato);

    @Modifying
    @Query("update dialog set venter_pa_svar_fra_bruker = null, eldste_uleste_for_veileder = :eldsteUlesteForVeileder, "
            + "venter_pa_nav_siden = :venterPaNavSiden, oppdatert = current_timestamp where dialog_id = :dialogId")
    void setNyMeldingFraBruker(@Param("dialogId") long dialogId,
                               @Param("eldsteUlesteForVeileder") LocalDateTime eldsteUlesteForVeileder,
                               @Param("venterPaNavSiden") LocalDateTime venterPaNavSiden);

    @Modifying
    @Query("update dialog set venter_pa_svar_fra_bruker = null, venter_pa_nav_siden = null, historisk = 1, "
            + "oppdatert = current_timestamp where dialog_id = :dialogId")
    void setHistorisk(@Param("dialogId") long dialogId);

    @Modifying
    @Query("update dialog set ulestparagraf8varsel = 1 where dialog_id = :dialogId")
    void markerSomParagraf8(@Param("dialogId") long dialogId);
}
