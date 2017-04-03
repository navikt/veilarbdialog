package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.binding.*;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

import java.util.List;

import static java.util.Optional.of;

@WebService
@Service
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
        String brukerIdent = hentDialogerForBrukerRequest.getPersonIdent();
        return of(brukerIdent)
                .map(appService::hentDialogerForBruker)
                .map(dialogData -> hentDialogerForBrukerResponse(dialogData, brukerIdent))
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
        String personIdent = SubjectHandler.getSubjectHandler().getUid();
        HentDialogMedIdResponse hentDialogMedIdResponse = new HentDialogMedIdResponse();
        hentDialogMedIdResponse.getDialogListe().add(soapServiceMapper.somWSDialog(appService.hentDialog(Long.parseLong(hentDialogMedIdRequest.getDialogId())), personIdent));
        return hentDialogMedIdResponse;
    }

    @Override
    public void markerDialogSomLest(MarkerDialogSomLestRequest markerDialogSomLestRequest)
            throws MarkerDialogSomLestSikkerhetsbegrensning, MarkerDialogSomLestUgyldigInput {
        throw new RuntimeException("not implemented");
    }

    @Override
    public OpprettDialogForAktivitetResponse opprettDialogForAktivitet(OpprettDialogForAktivitetRequest opprettDialogForAktivitetRequest)
            throws OpprettDialogForAktivitetSikkerhetsbegrensning, OpprettDialogForAktivitetUgyldigInput {
        throw new RuntimeException("not implemented");
    }

    @Override
    public OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplan(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest)
            throws OpprettDialogForAktivitetsplanPersonIkkeFunnet, OpprettDialogForAktivitetsplanSikkerhetsbegrensning, OpprettDialogForAktivitetsplanUgyldigInput {
        String personIdent = opprettDialogForAktivitetsplanRequest.getPersonIdent();
        return of(opprettDialogForAktivitetsplanRequest)
                .map(soapServiceMapper::somDialogData)
                .map(dialogData -> appService.opprettDialogForAktivitetsplanPaIdent(dialogData, personIdent))
                .map(this::opprettDialogForAktivitetsplanResponse)
                .get();
    }

    private OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplanResponse(DialogData dialogData) {
        OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplanResponse = new OpprettDialogForAktivitetsplanResponse();
        opprettDialogForAktivitetsplanResponse.setDialogId(Long.toString(dialogData.id));
        return opprettDialogForAktivitetsplanResponse;
    }

    @Override
    public void opprettHenvendelseForDialog(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest)
            throws OpprettHenvendelseForDialogSikkerhetsbegrensning, OpprettHenvendelseForDialogUgyldigInput {
        of(opprettHenvendelseForDialogRequest)
                .map(soapServiceMapper::somHenvendelseData)
                .ifPresent(dialogData -> appService.opprettHenvendelseForDialog(dialogData));
    }

}

