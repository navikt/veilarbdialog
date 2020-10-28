package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.abac.Pep;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.types.feil.IngenTilgang;
import no.nav.common.types.feil.UlovligHandling;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.feed.KvpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class DialogDataService {

    private final AktorregisterClient aktorregisterClient;
    private final DialogDAO dialogDAO;
    private final DialogStatusService dialogStatusService;
    private final DataVarehusDAO dataVarehusDAO;
    private final DialogFeedDAO dialogFeedDAO;
    private final Pep pep;
    private final KvpService kvpService;
    private final UnleashService unleashService;
    private final KafkaDialogService kafkaDialogService;
    private final AuthService auth;

    private String assertAccessToAktorId(String aktorId)
            throws IngenTilgang {
        if (!auth.activeUserHasReadAccessToPerson(aktorId)) {
            throw new IngenTilgang(String.format(
                    "%s har ikke lesetilgang til %s",
                    auth.getIdent().orElse("null"),
                    aktorId
            ));
        }
        return aktorId;
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(Person person)
            throws IngenTilgang {
        String aktorId = assertAccessToAktorId(hentAktoerIdForPerson(person));
        return dialogDAO.hentDialogerForAktorId(aktorId);
    }

    public Date hentSistOppdatertForBruker(Person person, String meg)
            throws IngenTilgang {
        String aktorId = assertAccessToAktorId(hentAktoerIdForPerson(person));
        return dataVarehusDAO.hentSisteEndringSomIkkeErDine(aktorId, meg);
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData)
            throws IngenTilgang {
        String aktorId = assertAccessToAktorId(dialogData.getAktorId());
        DialogData kontorsperretDialog = dialogData.withKontorsperreEnhetId(kvpService.kontorsperreEnhetId(aktorId));
        DialogData opprettet = dialogDAO.opprettDialog(kontorsperretDialog);
        dialogStatusService.nyDialog(opprettet);
        return opprettet;
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        long dialogId = henvendelseData.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        HenvendelseData henvendelse = henvendelseData
                .withKontorsperreEnhetId(kvpService.kontorsperreEnhetId(dialogData.getAktorId()));

        HenvendelseData opprettet = dialogDAO.opprettHenvendelse(henvendelse);
        return dialogStatusService.nyHenvendelse(dialogData, opprettet);
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
                    .ofNullable(aktorregisterClient.hentAktorId(person.get()))
                    .orElseThrow(RuntimeException::new);
        } else if (person instanceof Person.AktorId) {
            return person.get();
        } else {
            throw new RuntimeException("Kan ikke identifisere persontype");
        }
    }

    @Transactional(readOnly = true)
    public List<DialogAktor> hentAktorerMedEndringerFOM(Date tidspunkt, int pageSize) {
        // NB: ingen tilgangskontroll her siden feed har egen mekanisme for dette
        if (!this.unleashService.isEnabled("veilarbdialog.skruav.feed")) {
            return dialogFeedDAO.hentAktorerMedEndringerFOM(tidspunkt, pageSize);
        }
        return Collections.emptyList();
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
        if (unleashService.isEnabled("veilarbdialog.kafka1")) {
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

        if (!auth.activeUserHasReadAccessToPerson(dialogData.getAktorId())) {
            throw new IngenTilgang(String.format(
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
        if (dialogData.isHistorisk()) {
            throw new UlovligHandling();
        }
        return dialogData;
    }

    public void markerSomParagra8(long dialogId) {
        dialogStatusService.markerSomParagraf8(dialogId);
    }
}
