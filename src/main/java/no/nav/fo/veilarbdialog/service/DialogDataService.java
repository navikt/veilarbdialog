package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.SistePeriodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DialogDataService {

    private final AktorOppslagClient aktorOppslagClient;
    private final DialogDAO dialogDAO;
    private final DialogStatusService dialogStatusService;
    private final DataVarehusDAO dataVarehusDAO;
    private final KvpService kvpService;
    private final KafkaProducerService kafkaProducerService;
    private final AuthService auth;
    private final KladdService kladdService;
    private final FunksjonelleMetrikker funksjonelleMetrikker;
    private final SistePeriodeService sistePeriodeService;

    private final BrukernotifikasjonService brukernotifikasjonService;

    @Value("${application.dialog.url}")
    private String dialogUrl;


    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(Person person) {
        String aktorId = hentAktoerIdForPerson(person);
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        return dialogDAO.hentDialogerForAktorId(aktorId);
    }

    public Date hentSistOppdatertForBruker(Person person, String meg) {
        String aktorId = hentAktoerIdForPerson(person);
        String aktorEllerIdentInnloggetBruker = auth.erEksternBruker() ? hentAktoerIdForPerson(Person.fnr(meg)) : meg;
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        return dataVarehusDAO.hentSisteEndringSomIkkeErDine(aktorId, aktorEllerIdentInnloggetBruker);
    }

    @Transactional
    public DialogData opprettHenvendelse(NyHenvendelseDTO henvendelseData, Person bruker) {
        var aktivitetsId = AktivitetId.of(henvendelseData.getAktivitetId());
        String aktorId = hentAktoerIdForPerson(bruker);
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        Fnr fnr = hentFnrForPerson(bruker);
        if (!brukernotifikasjonService.kanVarsles(fnr)) {
            throw new ResponseStatusException(CONFLICT, "Bruker kan ikke varsles.");
        }

        DialogData dialog = Optional.ofNullable(hentDialogMedTilgangskontroll(henvendelseData.getDialogId(), aktivitetsId))
                .orElseGet(() -> opprettDialog(henvendelseData, aktorId));

        slettKladd(henvendelseData, bruker);

        opprettHenvendelseForDialog(dialog, henvendelseData.getEgenskaper() != null && !henvendelseData.getEgenskaper().isEmpty(), henvendelseData.getTekst());
        dialog = markerDialogSomLest(dialog.getId());

        sendPaaKafka(aktorId);

        return dialog;
    }

    public DialogData markerDialogSomLest(long dialogId) {
        if (auth.erEksternBruker()) {
            return markerDialogSomLestAvBruker(dialogId);
        }
        return markerDialogSomLestAvVeileder(dialogId);

    }

    public DialogData oppdaterFerdigbehandletTidspunkt(long dialogId, boolean ferdigBehandlet) {
        var dialogData = hentDialogMedSkrivetilgangskontroll(dialogId);
        return dialogStatusService.oppdaterVenterPaNavSiden(dialogData, ferdigBehandlet);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        var dialogData = hentDialogMedSkrivetilgangskontroll(dialogId);
        return dialogStatusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);
    }

    public void markerSomParagra8(long dialogId) {
        dialogStatusService.markerSomParagraf8(dialogId);
    }

    @Transactional(readOnly = true)
    public DialogData hentDialogMedTilgangskontroll(long dialogId) {
        var dialogData = hentDialogUtenTilgangskontroll(dialogId);
        sjekkLeseTilgangTilDialog(dialogData);
        return dialogData;
    }

    private DialogData opprettHenvendelseForDialog(DialogData dialogData, boolean viktigMelding, String tekst) {
        HenvendelseData opprettet = dialogDAO.opprettHenvendelse(HenvendelseData.builder()
                .dialogId(dialogData.getId())
                .avsenderId(auth.getIdent().orElse(null))
                .viktig(viktigMelding)
                .avsenderType(auth.erEksternBruker() ? AvsenderType.BRUKER : AvsenderType.VEILEDER)
                .tekst(tekst)
                .kontorsperreEnhetId(kvpService.kontorsperreEnhetId(dialogData.getAktorId()))
                .sendt(new Date())
                .build());

        return dialogStatusService.nyHenvendelse(dialogData, opprettet);
    }

    private DialogData hentDialogMedSkrivetilgangskontroll(long id) {
        var dialogData = hentDialogMedTilgangskontroll(id);
        if (dialogData.isHistorisk()) {
            throw new ResponseStatusException(CONFLICT);
        }
        return dialogData;
    }

    public DialogData hentDialogMedTilgangskontroll(String dialogId, AktivitetId aktivitetId) {
        if (dialogId == null && aktivitetId == null) return null;

        if (dialogId != null && !dialogId.isEmpty()) {
            return hentDialogMedTilgangskontroll(Long.parseLong(dialogId));
        } else {
            return Optional.ofNullable(aktivitetId)
                    .filter(a -> StringUtils.isNotEmpty(a.getId()))
                    .flatMap(this::hentDialogForAktivitetId)
                    .orElse(null);
        }
    }

    private DialogData markerDialogSomLestAvVeileder(long dialogId) {
        var dialogData = hentDialogMedTilgangskontroll(dialogId);
        return dialogStatusService.markerSomLestAvVeileder(dialogData);
    }

    private DialogData markerDialogSomLestAvBruker(long dialogId) {
        var dialogData = hentDialogMedTilgangskontroll(dialogId);
        return dialogStatusService.markerSomLestAvBruker(dialogData);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(AktivitetId aktivitetId) {
        return dialogDAO.hentDialogForAktivitetId(aktivitetId).map(this::sjekkLeseTilgangTilDialog);
    }

    public String hentAktoerIdForPerson(Person person) {
        if (person instanceof Person.Fnr) {
            return Optional
                    .ofNullable(aktorOppslagClient.hentAktorId(Fnr.of(person.get())))
                    .map(Id::get)
                    .orElseThrow(RuntimeException::new);
        } else if (person instanceof Person.AktorId) {
            return person.get();
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
        // NB: ingen tilgangskontroll, brukes av vår feed-consumer
        dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(aktoerId, avsluttetDato)
                .forEach(this::oppdaterDialogTilHistorisk);

        sendPaaKafka(aktoerId);
    }

    public void settDialogerTilHistoriske(String aktoerId, Date avsluttetDato) {
        // NB: ingen tilgangskontroll, brukes av vår feed-consumer
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

    private DialogData hentDialogUtenTilgangskontroll(long dialogId) {
        return dialogDAO.hentDialog(dialogId);
    }

    private void oppdaterDialogTilHistorisk(DialogData dialogData) {
        dialogStatusService.settDialogTilHistorisk(dialogData);
    }

    private DialogData sjekkLeseTilgangTilDialog(DialogData dialogData) {

        if (dialogData != null && !auth.harTilgangTilPerson(AktorId.of(dialogData.getAktorId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format(
                    "%s har ikke lesetilgang til %s",
                    auth.getIdent().orElse(null),
                    dialogData.getAktorId())
            );
        }
        return dialogData;

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
