package no.nav.fo.veilarbdialog.service;

import io.getunleash.Unleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV;
import static no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DialogDataService {

    private final AktorOppslagClient aktorOppslagClient;
    private final DialogVarslerClient dialogVarslerClient;
    private final DialogDAO dialogDAO;
    private final DialogStatusService dialogStatusService;
    private final DataVarehusDAO dataVarehusDAO;
    private final KvpService kvpService;
    private final KafkaProducerService kafkaProducerService;
    private final IAuthService auth;
    private final KladdService kladdService;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final SistePeriodeService sistePeriodeService;
    private final Unleash unleash;

    private final BrukernotifikasjonService brukernotifikasjonService;

    @Value("${application.dialog.url}")
    private String dialogUrl;


    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(Person person) {
        AktorId aktorId = hentAktoerIdForPerson(person);
        return dialogDAO.hentDialogerForAktorId(aktorId.get());
    }

    public Date hentSistOppdatertForBruker(Person person, Id endretAvId) {
        Id endretAv = auth.erEksternBruker() ? hentAktoerIdForPerson(endretAvId) : endretAvId;
        AktorId bruker = hentAktoerIdForPerson(person);
        return dataVarehusDAO.hentSisteEndringSomIkkeErDine(bruker.get(), endretAv.get());
    }

    @Transactional
    public DialogData opprettHenvendelse(NyHenvendelseDTO henvendelseData, Person bruker) {
        var aktivitetsId = AktivitetId.of(henvendelseData.getAktivitetId());
        AktorId aktorId = hentAktoerIdForPerson(bruker);
        Fnr fnr = hentFnrForPerson(bruker);
        if (!brukernotifikasjonService.kanVarsles(fnr)) {
            throw new ResponseStatusException(CONFLICT, "Bruker kan ikke varsles.");
        }

        DialogData dialog = Optional.ofNullable(hentDialog(henvendelseData.getDialogId(), aktivitetsId))
                .orElseGet(() -> opprettDialog(henvendelseData, aktorId.get()));

        slettKladd(henvendelseData, bruker);

        opprettHenvendelseForDialog(dialog, henvendelseData.getEgenskaper() != null && !henvendelseData.getEgenskaper().isEmpty(), henvendelseData.getTekst());
        dialog = markerDialogSomLest(dialog.getId());

        sendPaaKafka(aktorId.get());

        if (unleash.isEnabled("veilarbdialog.dialogvarsling")) {
            var eventType = auth.erEksternBruker() ? NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV : NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER;
            dialogVarslerClient.varsleLyttere(fnr, eventType);
        }

        return dialog;
    }

    public DialogData markerDialogSomLest(long dialogId) {
        if (auth.erEksternBruker()) {
            return markerDialogSomLestAvBruker(dialogId);
        }
        return markerDialogSomLestAvVeileder(dialogId);

    }

    public DialogData oppdaterFerdigbehandletTidspunkt(long dialogId, boolean ferdigBehandlet) {
        var dialogData = hentDialogSomKanOppdateres(dialogId);
        return dialogStatusService.oppdaterVenterPaNavSiden(dialogData, ferdigBehandlet);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        var dialogData = hentDialogSomKanOppdateres(dialogId);
        return dialogStatusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);
    }

    public void markerSomParagra8(long dialogId) {
        dialogStatusService.markerSomParagraf8(dialogId);
    }

    private DialogData opprettHenvendelseForDialog(DialogData dialogData, boolean viktigMelding, String tekst) {
        HenvendelseData opprettet = dialogDAO.opprettHenvendelse(HenvendelseData.builder()
                .dialogId(dialogData.getId())
                .avsenderId(auth.getLoggedInnUser().get())
                .viktig(viktigMelding)
                .avsenderType(auth.erEksternBruker() ? AvsenderType.BRUKER : AvsenderType.VEILEDER)
                .tekst(tekst)
                .kontorsperreEnhetId(kvpService.kontorsperreEnhetId(dialogData.getAktorId()))
                .sendt(new Date())
                .build());

        return dialogStatusService.nyHenvendelse(dialogData, opprettet);
    }

    private DialogData hentDialogSomKanOppdateres(long id) {
        var dialogData = hentDialog(id);
        if (dialogData.isHistorisk()) {
            throw new ResponseStatusException(CONFLICT);
        }
        return dialogData;
    }

    public DialogData hentDialog(String dialogId, AktivitetId aktivitetId) {
        if (dialogId == null && aktivitetId == null) return null;

        if (dialogId != null && !dialogId.isEmpty()) {
            return hentDialog(Long.parseLong(dialogId));
        } else {
            return Optional.ofNullable(aktivitetId)
                    .filter(a -> StringUtils.isNotEmpty(a.getId()))
                    .flatMap(this::hentDialogForAktivitetId)
                    .orElse(null);
        }
    }

    private DialogData markerDialogSomLestAvVeileder(long dialogId) {
        var dialogData = hentDialog(dialogId);
        return dialogStatusService.markerSomLestAvVeileder(dialogData);
    }

    private DialogData markerDialogSomLestAvBruker(long dialogId) {
        var dialogData = hentDialog(dialogId);
        return dialogStatusService.markerSomLestAvBruker(dialogData);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(AktivitetId aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId);
    }

    public AktorId hentAktoerIdForPerson(Person person) {
        return hentAktoerIdForPerson(person.eksternBrukerId());
    }

    public AktorId hentAktoerIdForPerson(Id person) {
        if (person instanceof Fnr) {
            return Optional
                    .ofNullable(aktorOppslagClient.hentAktorId(Fnr.of(person.get())))
                    .orElseThrow(RuntimeException::new);
        } else if (person instanceof AktorId) {
            return AktorId.of(person.get());
        }
        return null;
    }

    public Fnr hentFnrForPerson(Person person) {
        if (person instanceof Person.AktorId) {
            return Optional
                    .ofNullable(aktorOppslagClient.hentFnr(AktorId.of(person.get())))
                    .orElseThrow(RuntimeException::new);
        } else if (person instanceof Person.Fnr) {
            return Fnr.of(person.get());
        }
        return null;
    }

    public void settKontorsperredeDialogerTilHistoriske(String aktoerId, Date avsluttetDato) {
        dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(aktoerId, avsluttetDato)
                .forEach(this::oppdaterDialogTilHistorisk);

        sendPaaKafka(aktoerId);
    }

    public void settDialogerTilHistoriske(String aktoerId, Date avsluttetDato) {
        dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktoerId, avsluttetDato)
                .forEach(this::oppdaterDialogTilHistorisk);

        sendPaaKafka(aktoerId);
    }

    public void sendPaaKafka(String aktorId) {
        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        var kafkaDialogMelding = KafkaDialogMelding.mapTilDialogData(dialoger, aktorId);
        kafkaProducerService.sendDialogMelding(kafkaDialogMelding);
    }

    public void updateDialogEgenskap(EgenskapType type, long dialogId) {
        dialogDAO.updateDialogEgenskap(type, dialogId);
    }

    public DialogData hentDialog(long dialogId) {
        return dialogDAO.hentDialog(dialogId);
    }

    private void oppdaterDialogTilHistorisk(DialogData dialogData) {
        dialogStatusService.settDialogTilHistorisk(dialogData);
    }

    public DialogData opprettDialog(NyHenvendelseDTO nyHenvendelseDTO, String aktorId) {
        UUID gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(AktorId.of(aktorId));
        var dialogData = DialogData.builder()
                .oppfolgingsperiode(gjeldendeOppfolgingsperiode)
                .overskrift(nyHenvendelseDTO.getOverskrift())
                .aktorId(aktorId)
                .aktivitetId(AktivitetId.of(nyHenvendelseDTO.getAktivitetId()))
                .egenskaper(Optional.ofNullable(nyHenvendelseDTO.getEgenskaper())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(egenskap -> EgenskapType.valueOf(egenskap.name()))
                        .collect(Collectors.toList()))
                .kontorsperreEnhetId(kvpService.kontorsperreEnhetId(aktorId))
                .opprettetDato(new Date())
                .build();

        DialogData nyDialog = dialogDAO.opprettDialog(dialogData);
        dialogStatusService.oppdaterDatavarehus(nyDialog);

        if (auth.erEksternBruker()) {
            funksjonelleMetrikker.nyDialogBruker(nyDialog);
        } else if (auth.erInternBruker()) {
            funksjonelleMetrikker.nyDialogVeileder(nyDialog);
        }


        return nyDialog;
    }

    private void slettKladd(NyHenvendelseDTO nyHenvendelseDTO, Person person) {
        if (person instanceof Person.Fnr) {
            kladdService.deleteKladd(person.get(), nyHenvendelseDTO.getDialogId(), nyHenvendelseDTO.getAktivitetId());
        }
    }

    @SneakyThrows
    public URL utledDialogLink(long id) {
        return new URL(String.format("%s/%s", dialogUrl, id));
    }
}
