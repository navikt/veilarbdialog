package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.metrics.*;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.binding.*;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static no.nav.fo.veilarbdialog.util.StringUtils.notNullOrEmpty;

@Service
@SoapTjeneste("/Dialog")
public class SoapService implements AktivitetDialogV1 {

    @Inject
    private AppService appService;

    @Inject
    private SoapServiceMapper soapServiceMapper;

    @Override
    public void ping() {
    }

    @Override
    public HentDialogerForBrukerResponse hentDialogerForBruker(HentDialogerForBrukerRequest hentDialogerForBrukerRequest)
            throws HentDialogerForBrukerPersonIkkeFunnet, HentDialogerForBrukerSikkerhetsbegrensning, HentDialogerForBrukerUgyldigInput {
        String personIdent = hentDialogerForBrukerRequest.getPersonIdent();
        return of(personIdent)
                .map(appService::hentDialogerForBruker)
                .map(dialogData -> hentDialogerForBrukerResponse(dialogData, personIdent))
                .orElseThrow(RuntimeException::new);
    }

    private HentDialogerForBrukerResponse hentDialogerForBrukerResponse(List<DialogData> dialogData, String personIdent) {
        HentDialogerForBrukerResponse hentDialogerForBrukerResponse = new HentDialogerForBrukerResponse();
        dialogData.stream().map(dialog -> soapServiceMapper.somWSDialog(dialog, personIdent)).forEach(hentDialogerForBrukerResponse.getDialogListe()::add);
        return hentDialogerForBrukerResponse;
    }

    @Override
    public HentDialogerForAktivitetResponse hentDialogerForAktivitet(HentDialogerForAktivitetRequest hentDialogerForAktivitetRequest)
            throws HentDialogerForAktivitetSikkerhetsbegrensning, HentDialogerForAktivitetUgyldigInput {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public HentDialogMedIdResponse hentDialogMedId(HentDialogMedIdRequest hentDialogMedIdRequest)
            throws HentDialogMedIdSikkerhetsbegrensning, HentDialogMedIdUgyldigInput {
        String personIdent = getPersonIdent();
        HentDialogMedIdResponse hentDialogMedIdResponse = new HentDialogMedIdResponse();
        hentDialogMedIdResponse.getDialogListe().add(soapServiceMapper.somWSDialog(appService.hentDialog(Long.parseLong(hentDialogMedIdRequest.getDialogId())), personIdent));
        return hentDialogMedIdResponse;
    }

    @Override
    public void markerDialogSomLest(MarkerDialogSomLestRequest markerDialogSomLestRequest)
            throws MarkerDialogSomLestSikkerhetsbegrensning, MarkerDialogSomLestUgyldigInput {
        DialogData dialogData = appService.markerDialogSomLestAvBruker(Long.parseLong(markerDialogSomLestRequest.getDialogId()));
        createMetricsDialogLest(dialogData);
    }

    private void createMetricsDialogLest(DialogData dialogData) {
        long msSidenMelding = new Date().getTime() - dialogData.getSisteEndring().getTime();
        MetricsFactory
                .createEvent("MarkerSomLestBruker")
                .addFieldToReport("ms", msSidenMelding)
                .setSuccess()
                .report();
    }

    private DialogData updateDialogAktorFor(DialogData dialogData) {
        appService.updateDialogAktorFor(dialogData.getAktorId());
        return dialogData;
    }

    private DialogData markerDialogSomLest(DialogData dialogData) {
        return appService.markerDialogSomLestAvBruker(dialogData.getId());
    }

    @Override
    public OpprettDialogForAktivitetResponse opprettDialogForAktivitet(OpprettDialogForAktivitetRequest opprettDialogForAktivitetRequest)
            throws OpprettDialogForAktivitetSikkerhetsbegrensning, OpprettDialogForAktivitetUgyldigInput {
        String personIdent = getPersonIdent();

        return of(opprettDialogForAktivitetRequest)
                .map(r -> soapServiceMapper.somDialogData(opprettDialogForAktivitetRequest, personIdent))
                .map(appService::opprettDialogForAktivitetsplanPaIdent)
                .map(this::markerDialogSomLest)
                .map(this::updateDialogAktorFor)
                .map(this::createMetricsNyDialog)
                .map(this::opprettDialogForAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    private OpprettDialogForAktivitetResponse opprettDialogForAktivitetResponse(DialogData dialogData) {
        OpprettDialogForAktivitetResponse opprettDialogForAktivitetResponse = new OpprettDialogForAktivitetResponse();
        opprettDialogForAktivitetResponse.setDialogId(Long.toString(dialogData.getId()));
        return opprettDialogForAktivitetResponse;
    }

    @Override
    public OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplan(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest)
            throws OpprettDialogForAktivitetsplanPersonIkkeFunnet, OpprettDialogForAktivitetsplanSikkerhetsbegrensning, OpprettDialogForAktivitetsplanUgyldigInput {
        return of(opprettDialogForAktivitetsplanRequest)
                .map(soapServiceMapper::somDialogData)
                .map(dialogData -> appService.opprettDialogForAktivitetsplanPaIdent(dialogData))
                .map(this::markerDialogSomLest)
                .map(this::updateDialogAktorFor)
                .map(this::createMetricsNyDialog)
                .map(this::opprettDialogForAktivitetsplanResponse)
                .orElseThrow(RuntimeException::new);
    }

    private DialogData createMetricsNyDialog(DialogData dialogData) {
        MetricsFactory
                .createEvent("NyDialogBruker")
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialogData.getAktivitetId()))
                .setSuccess()
                .report();
        return dialogData;
    }

    private OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplanResponse(DialogData dialogData) {
        OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplanResponse = new OpprettDialogForAktivitetsplanResponse();
        opprettDialogForAktivitetsplanResponse.setDialogId(Long.toString(dialogData.getId()));
        return opprettDialogForAktivitetsplanResponse;
    }

    @Override
    public void opprettHenvendelseForDialog(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest)
            throws OpprettHenvendelseForDialogSikkerhetsbegrensning, OpprettHenvendelseForDialogUgyldigInput {
        String personIdent = getPersonIdent();

        of(opprettHenvendelseForDialogRequest)
                .map(r -> soapServiceMapper.somHenvendelseData(r, personIdent))
                .map(appService::opprettHenvendelseForDialog)
                .map(this::markerDialogSomLest)
                .map(this::createNyMeldingMetrics)
                .ifPresent(this::updateDialogAktorFor);
    }

    private DialogData createNyMeldingMetrics(DialogData dialogData) {
        Event event = MetricsFactory
                .createEvent("NyMeldingBruker")
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialogData.getAktivitetId()));
        if (isSvartpaa(dialogData))
            event.addFieldToReport("svartid", svartid(dialogData));

        event.report();
        return dialogData;
    }

    private long svartid(DialogData dialogData) {
        return new Date().getTime() - dialogData.getVenterPaSvarTidspunkt().getTime();
    }

    private boolean isSvartpaa(DialogData dialogData) {
        Date venterPaSvarTidspunkt = dialogData.getVenterPaSvarTidspunkt();
        if (venterPaSvarTidspunkt == null)
            return false;

        List collect = dialogData.getHenvendelser().stream()
                .filter(h -> h.getAvsenderType() == AvsenderType.BRUKER)
                .filter(h -> venterPaSvarTidspunkt.before(h.getSendt()))
                .collect(Collectors.toList());

        return collect.size() == 1;
    }

    private String getPersonIdent() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

}

