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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(Person person) {
        String aktorId = hentAktoerIdForPerson(person);
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        return dialogDAO.hentDialogerForAktorId(aktorId);
    }

    public Date hentSistOppdatertForBruker(Person person, String meg) {
        String aktorId = hentAktoerIdForPerson(person);
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        return dataVarehusDAO.hentSisteEndringSomIkkeErDine(aktorId, meg);
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData) {
        String aktorId = dialogData.getAktorId();
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        DialogData kontorsperretDialog = dialogData.withKontorsperreEnhetId(kvpService.kontorsperreEnhetId(aktorId));
        DialogData opprettet = dialogDAO.opprettDialog(kontorsperretDialog);
        dialogStatusService.nyDialog(opprettet);
        return opprettet;
    }

    public void opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        long dialogId = henvendelseData.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        HenvendelseData henvendelse = henvendelseData
                .withKontorsperreEnhetId(kvpService.kontorsperreEnhetId(dialogData.getAktorId()));

        dialogDAO.opprettHenvendelse(henvendelse);
    }

    @Transactional(readOnly = true)
    public DialogData hentDialog(long dialogId) {
        DialogData dialogData = hentDialogUtenTilgangskontroll(dialogId);
        sjekkLeseTilgangTilDialog(dialogData);
        return dialogData;
    }

    public DialogData markerDialogSomLestAvVeileder(long dialogId) {
        DialogData dialogData = sjekkLeseTilgangTilDialog(dialogId);
        return dialogStatusService.markerSomLestAvVeileder(dialogData);
    }

    public DialogData markerDialogSomLestAvBruker(long dialogId) {
        DialogData dialogData = sjekkLeseTilgangTilDialog(dialogId);
        return dialogStatusService.markerSomLestAvBruker(dialogData);
    }

    public DialogData oppdaterFerdigbehandletTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        return dialogStatusService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);
    }

    public DialogData oppdaterVentePaSvarTidspunkt(DialogStatus dialogStatus) {
        long dialogId = dialogStatus.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        return dialogStatusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);
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

        updateDialogAktorFor(aktoerId);
    }

    public void settDialogerTilHistoriske(String aktoerId, Date avsluttetDato) {
        // NB: ingen tilgangskontroll, brukes av vår feed-consumer
        dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktoerId, avsluttetDato)
                .forEach(this::oppdaterDialogTilHistorisk);

        updateDialogAktorFor(aktoerId);
    }

    public void updateDialogAktorFor(String aktorId) {
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

    private DialogData sjekkLeseTilgangTilDialog(long id) {
        return hentDialog(id);
    }

    private DialogData sjekkSkriveTilgangTilDialog(long id) {
        DialogData dialogData = hentDialog(id);
        if (dialogData != null && dialogData.isHistorisk()) {
            throw new ResponseStatusException(CONFLICT);
        }
        return dialogData;
    }

    public void markerSomParagra8(long dialogId) {
        dialogStatusService.markerSomParagraf8(dialogId);
    }
}
