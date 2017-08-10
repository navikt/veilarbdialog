package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.binding.*;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

import java.util.List;

import static java.util.Optional.of;

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
                .get();
    }

    private HentDialogerForBrukerResponse hentDialogerForBrukerResponse(List<DialogData> dialogData, String personIdent) {
        HentDialogerForBrukerResponse hentDialogerForBrukerResponse = new HentDialogerForBrukerResponse();
        dialogData.stream().map(dialog -> soapServiceMapper.somWSDialog(dialog, personIdent)).forEach(hentDialogerForBrukerResponse.getDialogListe()::add);
        return hentDialogerForBrukerResponse;
    }

    @Override
    public HentDialogerForAktivitetResponse hentDialogerForAktivitet(HentDialogerForAktivitetRequest hentDialogerForAktivitetRequest)
            throws HentDialogerForAktivitetSikkerhetsbegrensning, HentDialogerForAktivitetUgyldigInput {
        throw new RuntimeException("not implemented");
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
        appService.markerDialogSomLestAvBruker(Long.parseLong(markerDialogSomLestRequest.getDialogId()));
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
                .map(this::opprettDialogForAktivitetResponse)
                .get();
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
                .map(this::opprettDialogForAktivitetsplanResponse)
                .get();
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
                .ifPresent(this::markerDialogSomLest);
    }

    private String getPersonIdent() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

}

