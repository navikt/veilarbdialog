package no.nav.fo.veilarbdialog.service;

import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.domain.KafkaDialogMelding;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class AppService {

    private final AktorService aktorService;
    private final DialogDAO dialogDAO;
    private final DialogStatusService dialogStatusService;
    private final DataVarehusDAO dataVarehusDAO;
    private final DialogFeedDAO dialogFeedDAO;
    private final PepClient pepClient;
    private final KvpClient kvpClient;
    private final UnleashService unleashService;
    private final KafkaDialogService kafkaDialogService;

    public AppService(AktorService aktorService,
                      DialogDAO dialogDAO,
                      DialogStatusService dialogStatusService,
                      DataVarehusDAO dataVarehusDAO,
                      DialogFeedDAO dialogFeedDAO,
                      PepClient pepClient,
                      KafkaDialogService kafkaDialogService,
                      KvpClient kvpClient,
                      UnleashService unleashService) {
        this.aktorService = aktorService;
        this.dialogDAO = dialogDAO;
        this.dialogStatusService = dialogStatusService;
        this.dataVarehusDAO = dataVarehusDAO;
        this.dialogFeedDAO = dialogFeedDAO;
        this.pepClient = pepClient;
        this.kvpClient = kvpClient;
        this.unleashService = unleashService;
        this.kafkaDialogService = kafkaDialogService;
    }

    @Transactional(readOnly = true)
    public List<DialogData> hentDialogerForBruker(Person person) {
        String aktorId = hentAktoerIdForPerson(person);
        pepClient.sjekkLesetilgangTilAktorId(aktorId);

        return dialogDAO.hentDialogerForAktorId(aktorId);
    }

    public Date hentSistOppdatertForBruker(Person person, String meg) {
        String aktorId = hentAktoerIdForPerson(person);
        pepClient.sjekkLesetilgangTilAktorId(aktorId);

        return dataVarehusDAO.hentSisteEndringSomIkkeErDine(aktorId, meg);
    }

    public DialogData opprettDialogForAktivitetsplanPaIdent(DialogData dialogData) {
        pepClient.sjekkLesetilgangTilAktorId(dialogData.getAktorId());
        DialogData kontorsperretDialog = dialogData.withKontorsperreEnhetId(kvpClient.kontorsperreEnhetId(dialogData.getAktorId()));
        DialogData oprettet = dialogDAO.opprettDialog(kontorsperretDialog);
        dialogStatusService.nyDialog(oprettet);
        return oprettet;
    }

    public DialogData opprettHenvendelseForDialog(HenvendelseData henvendelseData) {
        long dialogId = henvendelseData.dialogId;
        DialogData dialogData = sjekkSkriveTilgangTilDialog(dialogId);
        HenvendelseData henvendelse = henvendelseData
                .withKontorsperreEnhetId(kvpClient.kontorsperreEnhetId(dialogData.getAktorId()));

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
            return aktorService.getAktorId(person.get())
                    .orElseThrow(RuntimeException::new);
        } else if (person instanceof Person.AktorId) {
            return person.get();
        } else {
            throw new RuntimeException("Kan ikke identifisere persontype");
        }
    }

    public String hentFnrForPerson(Person person) {
        if (person instanceof Person.Fnr) {
            return person.get();
        } else if (person instanceof Person.AktorId) {
            return aktorService.getFnr(person.get())
                    .orElseThrow(RuntimeException::new);
        } else {
            throw new RuntimeException("Kan ikke identifisere persontype");
        }
    }

    @Transactional(readOnly = true)
    public List<DialogAktor> hentAktorerMedEndringerFOM(Date tidspunkt, int pageSize) {
        // NB: ingen tilgangskontroll her siden feed har egen mekanisme for dette
        if(!this.unleashService.isEnabled("veilarbdialog.skruav.feed")) {
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
        if(unleashService.isEnabled("veilarbdialog.kafka1")) {
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
        pepClient.sjekkLesetilgangTilAktorId(dialogData.getAktorId());
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
