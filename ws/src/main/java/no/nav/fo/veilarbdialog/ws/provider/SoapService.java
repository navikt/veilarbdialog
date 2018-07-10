package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.common.auth.SubjectHandler;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.binding.AktivitetDialogV1;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
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
        //tom pga Ping komentar lagt til pga sonar
    }

    @Override
    public HentDialogerForBrukerResponse hentDialogerForBruker(HentDialogerForBrukerRequest hentDialogerForBrukerRequest) {
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
    public HentDialogerForAktivitetResponse hentDialogerForAktivitet(HentDialogerForAktivitetRequest hentDialogerForAktivitetRequest) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public HentDialogMedIdResponse hentDialogMedId(HentDialogMedIdRequest hentDialogMedIdRequest) {
        String personIdent = getPersonIdent();
        HentDialogMedIdResponse hentDialogMedIdResponse = new HentDialogMedIdResponse();
        hentDialogMedIdResponse.getDialogListe().add(soapServiceMapper.somWSDialog(appService.hentDialog(Long.parseLong(hentDialogMedIdRequest.getDialogId())), personIdent));
        return hentDialogMedIdResponse;
    }

    @Override
    public void markerDialogSomLest(MarkerDialogSomLestRequest markerDialogSomLestRequest) {
        appService.markerDialogSomLestAvBruker(Long.parseLong(markerDialogSomLestRequest.getDialogId()));
    }

    @Override
    public OpprettDialogForAktivitetResponse opprettDialogForAktivitet(OpprettDialogForAktivitetRequest opprettDialogForAktivitetRequest) {
        String personIdent = getPersonIdent();

        return of(opprettDialogForAktivitetRequest)
                .map(r -> soapServiceMapper.somDialogData(opprettDialogForAktivitetRequest, personIdent))
                .map(appService::opprettDialogForAktivitetsplanPaIdent)
                .map(this::markerDialogSomLest)
                .map(this::updateDialogAktorFor)
                .map(FunksjonelleMetrikker::nyDialogBruker)
                .map(this::opprettDialogForAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    private OpprettDialogForAktivitetResponse opprettDialogForAktivitetResponse(DialogData dialogData) {
        OpprettDialogForAktivitetResponse opprettDialogForAktivitetResponse = new OpprettDialogForAktivitetResponse();
        opprettDialogForAktivitetResponse.setDialogId(Long.toString(dialogData.getId()));
        return opprettDialogForAktivitetResponse;
    }

    @Override
    public OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplan(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest) {
        return of(opprettDialogForAktivitetsplanRequest)
                .map(soapServiceMapper::somDialogData)
                .map(dialogData -> appService.opprettDialogForAktivitetsplanPaIdent(dialogData))
                .map(this::markerDialogSomLest)
                .map(this::updateDialogAktorFor)
                .map(FunksjonelleMetrikker::nyDialogBruker)
                .map(this::opprettDialogForAktivitetsplanResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public void opprettHenvendelseForDialog(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest) {
        String personIdent = getPersonIdent();
        of(opprettHenvendelseForDialogRequest)
                .map(r -> soapServiceMapper.somHenvendelseData(r, personIdent))
                .map(appService::opprettHenvendelseForDialog)
                .map(this::markerDialogSomLest)
                .ifPresent(this::updateDialogAktorFor);
    }

    private String getPersonIdent() {
        return SubjectHandler.getIdent().orElseThrow(IllegalStateException::new);
    }

    private OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplanResponse(DialogData dialogData) {
        OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplanResponse = new OpprettDialogForAktivitetsplanResponse();
        opprettDialogForAktivitetsplanResponse.setDialogId(Long.toString(dialogData.getId()));
        return opprettDialogForAktivitetsplanResponse;
    }

    private DialogData updateDialogAktorFor(DialogData dialogData) {
        appService.updateDialogAktorFor(dialogData.getAktorId());
        return dialogData;
    }

    private DialogData markerDialogSomLest(DialogData dialogData) {
        return appService.markerDialogSomLestAvBruker(dialogData.getId());
    }
}

