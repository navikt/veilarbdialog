package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.feed.KvpService;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Transactional
@RequiredArgsConstructor
public class DialogDataService {

    private final AktorOppslagClient aktorOppslagClient;
    private final DialogDAO dialogDAO;
    private final DialogStatusService dialogStatusService;
    private final DataVarehusDAO dataVarehusDAO;
    private final DialogFeedDAO dialogFeedDAO;
    private final KvpService kvpService;
    private final UnleashClient unleashClient;
    private final KafkaDialogService kafkaDialogService;
    private final AuthService auth;
    private final KladdService kladdService;
    private final FunksjonelleMetrikker funksjonelleMetrikker;


    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(Person person) {
        String aktorId = hentAktoerIdForPerson(person);
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        return dialogDAO.hentDialogerForAktorId(aktorId);
    }

    public Date hentSistOppdatertForBruker(Person person, String meg) {
        String aktorId = hentAktoerIdForPerson(person);
        String aktorEllerIdentInnloggetBruker = auth.erEksternBruker()? hentAktoerIdForPerson(Person.fnr(meg)): meg;
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        return dataVarehusDAO.hentSisteEndringSomIkkeErDine(aktorId, aktorEllerIdentInnloggetBruker);
    }

    @Transactional
    public DialogData opprettHenvendelse(NyHenvendelseDTO henvendelseData, Person bruker) {
        String aktorId = hentAktoerIdForPerson(bruker);
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);

        DialogData dialog = Optional.ofNullable(hentDialogMedTilgangskontroll(henvendelseData.dialogId, henvendelseData.aktivitetId))
                .orElseGet(() -> opprettDialog(henvendelseData, aktorId));

        slettKladd(henvendelseData, bruker);

        opprettHenvendelseForDialog(dialog, henvendelseData.egenskaper != null && !henvendelseData.egenskaper.isEmpty(), henvendelseData.tekst);
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

    public DialogData oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        DialogData dialogData = hentDialogMedSkrivetilgangskontroll(dialogId);
        return dialogStatusService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        DialogData dialogData = hentDialogMedSkrivetilgangskontroll(dialogId);
        return dialogStatusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);
    }

    public void markerSomParagra8(long dialogId) {
        dialogStatusService.markerSomParagraf8(dialogId);
    }

    @Transactional(readOnly = true)
    public DialogData hentDialogMedTilgangskontroll(long dialogId) {
        DialogData dialogData = hentDialogUtenTilgangskontroll(dialogId);
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
                .build());

        return dialogStatusService.nyHenvendelse(dialogData, opprettet);
    }

    private DialogData hentDialogMedSkrivetilgangskontroll(long id) {
        DialogData dialogData = hentDialogMedTilgangskontroll(id);
        if (dialogData.isHistorisk()) {
            throw new ResponseStatusException(CONFLICT);
        }
        return dialogData;
    }

    public DialogData hentDialogMedTilgangskontroll(String dialogId, String aktivitetId) {
        if(dialogId == null && aktivitetId == null) return null;

        if (dialogId != null && !dialogId.isEmpty()) {
            return hentDialogMedTilgangskontroll(Long.parseLong(dialogId));
        } else {
            return Optional.ofNullable(aktivitetId)
                    .filter(StringUtils::isNotEmpty)
                    .flatMap(this::hentDialogForAktivitetId)
                    .orElse(null);
        }
    }

    private DialogData markerDialogSomLestAvVeileder(long dialogId) {
        DialogData dialogData = hentDialogMedTilgangskontroll(dialogId);
        return dialogStatusService.markerSomLestAvVeileder(dialogData);
    }

    private DialogData markerDialogSomLestAvBruker(long dialogId) {
        DialogData dialogData = hentDialogMedTilgangskontroll(dialogId);
        return dialogStatusService.markerSomLestAvBruker(dialogData);
    }

    @Transactional(readOnly = true)
    public Optional<DialogData> hentDialogForAktivitetId(String aktivitetId) {
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
        } else {
            throw new RuntimeException("Kan ikke identifisere persontype");
        }
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
        if (unleashClient.isEnabled("veilarbdialog.kafka1")) {
            KafkaDialogMelding kafkaDialogMelding = KafkaDialogMelding.mapTilDialogData(dialoger, aktorId);
            kafkaDialogService.dialogEvent(kafkaDialogMelding);
        }
        dialogFeedDAO.updateDialogAktorFor(aktorId, dialoger);

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

        if (dialogData != null && !auth.harTilgangTilPerson(dialogData.getAktorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format(
                    "%s har ikke lesetilgang til %s",
                    auth.getIdent().orElse(null),
                    dialogData.getAktorId())
            );
        }
        return dialogData;

    }

    public DialogData opprettDialog(NyHenvendelseDTO nyHenvendelseDTO, String aktorId) {
        DialogData dialogData = DialogData.builder()
                .overskrift(nyHenvendelseDTO.overskrift)
                .aktorId(aktorId)
                .aktivitetId(nyHenvendelseDTO.aktivitetId)
                .egenskaper(Optional.ofNullable(nyHenvendelseDTO.egenskaper)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(egenskap -> EgenskapType.valueOf(egenskap.name()))
                        .collect(Collectors.toList()))
                .build();

        DialogData kontorsperretDialog = dialogData.withKontorsperreEnhetId(kvpService.kontorsperreEnhetId(aktorId));
        DialogData nyDialog = dialogDAO.opprettDialog(kontorsperretDialog);
        dialogStatusService.nyDialog(nyDialog);

        if (auth.erEksternBruker()) {
            funksjonelleMetrikker.nyDialogBruker(nyDialog);
        } else if (auth.erInternBruker()) {
            funksjonelleMetrikker.nyDialogVeileder(nyDialog);
        }


        return nyDialog;
    }

    private void slettKladd(NyHenvendelseDTO nyHenvendelseDTO, Person person) {
        if (person instanceof Person.Fnr) {
            kladdService.deleteKladd(person.get(), nyHenvendelseDTO.dialogId, nyHenvendelseDTO.aktivitetId);
        }
    }
}
