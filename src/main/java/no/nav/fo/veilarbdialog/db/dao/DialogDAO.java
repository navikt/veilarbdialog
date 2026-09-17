package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.domain.DialogId;
import no.nav.fo.veilarbdialog.db.jdbc.DialogEgenskapRepository;
import no.nav.fo.veilarbdialog.db.jdbc.DialogEntity;
import no.nav.fo.veilarbdialog.db.jdbc.DialogRepository;
import no.nav.fo.veilarbdialog.db.jdbc.HenvendelseEntity;
import no.nav.fo.veilarbdialog.db.jdbc.HenvendelseRepository;
import no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters;
import no.nav.fo.veilarbdialog.dialog.exceptions.AktivitetHarAlleredeDialogTrådException;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.parseUuidOrNull;
import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toBoolean;
import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toDate;
import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toInt;
import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toLocalDateTime;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DialogDAO {

    private final DialogRepository dialogRepository;
    private final HenvendelseRepository henvendelseRepository;
    private final DialogEgenskapRepository dialogEgenskapRepository;

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForAktorId(String aktorId) {
        return dialogRepository.findByAktorId(aktorId)
                .stream()
                .map(this::mapDialog)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForOppfolgingsperiodeId(UUID oppfolgingsperiodeId) {
        return dialogRepository.findByOppfolgingsperiodeUuid(oppfolgingsperiodeId.toString())
                .stream()
                .map(this::mapDialog)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(String aktorId, Date avsluttetDato) {
        return dialogRepository.findKontorsperredeSomSkalAvsluttes(aktorId, toLocalDateTime(avsluttetDato))
                .stream()
                .map(this::mapDialog)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerSomSkalAvsluttesForAktorId(String aktorId, UUID oppfolgingsperiodeId) {
        return dialogRepository.findSomSkalAvsluttes(aktorId, oppfolgingsperiodeId.toString())
                .stream()
                .map(this::mapDialog)
                .toList();
    }

    @Transactional(readOnly = true)
    public DialogData hentDialog(long dialogId) {
        return dialogRepository.findById(dialogId)
                .map(this::mapDialog)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public DialogData hentDialog(DialogId dialogId, AktorId aktorId) {
        return dialogRepository.findByDialogIdAndAktorId(dialogId.getValue(), aktorId.get())
                .map(this::mapDialog)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public DialogData hentDialogGittHenvendelse(long henvendelseId) {
        return dialogRepository.findByHenvendelseId(henvendelseId)
                .map(this::mapDialog)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public HenvendelseData hentHenvendelse(long id) {
        return henvendelseRepository.findById(id)
                .map(henvendelse -> dialogRepository.findById(henvendelse.getDialogId())
                        .map(dialog -> mapHenvendelse(henvendelse, dialog))
                        .orElseGet(() -> mapHenvendelse(henvendelse, null)))
                .orElse(null);
    }

    public int kasserHenvendelse(long id) {
        return henvendelseRepository.kasserHenvendelse(id);
    }

    public int kasserDialog(long id) {
        return dialogRepository.kasserDialog(id);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(AktivitetId aktivitetId, AktorId aktorId) {
        if (aktivitetId == null) {
            return Optional.empty();
        }
        List<DialogEntity> dialoger;
        if (aktivitetId instanceof TekniskId) {
            dialoger = dialogRepository.findByAktivitetIdAndAktorId(aktivitetId.getId(), aktorId.get());
        } else if (aktivitetId instanceof Arenaid) {
            dialoger = dialogRepository.findByArenaIdAndAktorId(aktivitetId.getId(), aktorId.get());
        } else {
            throw new UnsupportedOperationException("Uknown id-type");
        }
        return dialoger.stream().findFirst().map(this::mapDialog);
    }

    public DialogData opprettDialog(DialogData dialogData) {
        long dialogId = dialogRepository.nextDialogId();

        var hasId = dialogData.getAktivitetId() != null;
        var isTekniskId = dialogData.getAktivitetId() instanceof TekniskId;
        var arenaId = hasId && !isTekniskId ? dialogData.getAktivitetId().getId() : null;
        var tekniskId = hasId && isTekniskId ? dialogData.getAktivitetId().getId() : null;

        try {
            dialogRepository.insert(
                    dialogId,
                    dialogData.getAktorId(),
                    toLocalDateTime(dialogData.getOpprettetDato()),
                    tekniskId,
                    arenaId,
                    dialogData.getOverskrift(),
                    toInt(dialogData.isHistorisk()),
                    dialogData.getKontorsperreEnhetId(),
                    toLocalDateTime(dialogData.getOpprettetDato()),
                    JdbcConverters.toString(dialogData.getOppfolgingsperiode())
            );
        } catch (DuplicateKeyException e) {
            var aktivitetid = dialogData.getAktivitetId();
            throw new AktivitetHarAlleredeDialogTrådException(aktivitetid != null ? aktivitetid.getId() : null);
        }

        dialogData.getEgenskaper()
                .forEach(egenskapType -> updateDialogEgenskap(egenskapType, dialogId));

        log.info("opprettet dialog id:{}", dialogId);
        return hentDialog(dialogId);
    }

    public void updateDialogEgenskap(EgenskapType type, long dialogId) {
        dialogEgenskapRepository.insert(dialogId, type.toString());
    }

    public HenvendelseData opprettHenvendelse(HenvendelseData henvendelseData) {
        long henvendelseId = henvendelseRepository.nextHenvendelseId();

        henvendelseRepository.insert(
                henvendelseId,
                henvendelseData.dialogId,
                toLocalDateTime(henvendelseData.sendt),
                henvendelseData.tekst,
                henvendelseData.kontorsperreEnhetId,
                henvendelseData.avsenderId,
                EnumUtils.getName(henvendelseData.avsenderType),
                toInt(henvendelseData.viktig)
        );

        log.info("opprettet henvendelse id:{} data:{}", henvendelseId, henvendelseData);
        return hentHenvendelse(henvendelseId);
    }

    @Transactional(readOnly = true)
    public List<String> hentAktorIderTilBrukereMedAktiveDialoger() {
        return dialogRepository.findAktiveAktorIder();
    }

    private DialogData mapDialog(DialogEntity dialog) {
        long dialogId = dialog.getDialogId();
        List<EgenskapType> egenskaper = dialogEgenskapRepository.findTyperByDialogId(dialogId)
                .stream()
                .map(kode -> kode == null ? null : EgenskapType.valueOf(kode))
                .toList();

        String aktivitetId = dialog.getAktivitetId() != null ? dialog.getAktivitetId() : dialog.getArenaId();

        return DialogData.builder()
                .id(dialogId)
                .aktorId(dialog.getAktorId())
                .aktivitetId(AktivitetId.of(aktivitetId))
                .overskrift(dialog.getOverskrift())
                .lestAvBrukerTidspunkt(toDate(dialog.getLestAvBrukerTid()))
                .lestAvVeilederTidspunkt(toDate(dialog.getLestAvVeilederTid()))
                .henvendelser(hentHenvendelser(dialog))
                .historisk(toBoolean(dialog.getHistorisk()))
                .opprettetDato(toDate(dialog.getOpprettetDato()))
                .venterPaNavSiden(toDate(dialog.getVenterPaNavSiden()))
                .venterPaSvarFraBrukerSiden(toDate(dialog.getVenterPaSvarFraBruker()))
                .eldsteUlesteTidspunktForBruker(toDate(dialog.getEldsteUlesteForBruker()))
                .sisteUlestAvVeilederTidspunkt(toDate(dialog.getEldsteUlesteForVeileder()))
                .oppdatert(toDate(dialog.getOppdatert()))
                .kontorsperreEnhetId(dialog.getKontorsperreEnhetId())
                .egenskaper(egenskaper)
                .harUlestParagraf8Henvendelse(toBoolean(dialog.getUlestparagraf8varsel()))
                .paragraf8VarselUUID(dialog.getParagraf8VarselUuid())
                .oppfolgingsperiode(parseUuidOrNull(dialog.getOppfolgingsperiodeUuid()))
                .build();
    }

    private List<HenvendelseData> hentHenvendelser(DialogEntity dialog) {
        return henvendelseRepository.findByDialogId(dialog.getDialogId())
                .stream()
                .map(henvendelse -> mapHenvendelse(henvendelse, dialog))
                .toList();
    }

    private static HenvendelseData mapHenvendelse(HenvendelseEntity henvendelse, DialogEntity dialog) {
        Date henvendelseDato = toDate(henvendelse.getSendt());
        Date eldsteUlesteForBruker = dialog == null ? null : toDate(dialog.getEldsteUlesteForBruker());
        Date eldsteUlesteForVeileder = dialog == null ? null : toDate(dialog.getEldsteUlesteForVeileder());
        return HenvendelseData.builder()
                .id(henvendelse.getHenvendelseId())
                .dialogId(henvendelse.getDialogId())
                .sendt(henvendelseDato)
                .tekst(henvendelse.getTekst())
                .avsenderId(henvendelse.getAvsenderId())
                .avsenderType(EnumUtils.valueOf(AvsenderType.class, henvendelse.getAvsenderType()))
                .lestAvBruker(erLest(eldsteUlesteForBruker, henvendelseDato))
                .lestAvVeileder(erLest(eldsteUlesteForVeileder, henvendelseDato))
                .kontorsperreEnhetId(henvendelse.getKontorsperreEnhetId())
                .viktig(toBoolean(henvendelse.getViktig()))
                .build();
    }

    private static boolean erLest(Date eldsteUleste, Date henvendelseTidspunkt) {
        return eldsteUleste == null || henvendelseTidspunkt.before(eldsteUleste);
    }
}
