package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.binding.*;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

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
    public HentDialogerForBrukerResponse hentDialogerForBruker(HentDialogerForBrukerRequest hentDialogerForBrukerRequest) throws HentDialogerForBrukerPersonIkkeFunnet, HentDialogerForBrukerSikkerhetsbegrensning, HentDialogerForBrukerUgyldigInput {
        return of(hentDialogerForBrukerRequest)
                .map(soapServiceMapper::brukerIdent)
                .map(appService::hentDialogerForBruker)
                .map(soapServiceMapper::mapTil)
                .get();
    }

    @Override
    public HentDialogerForAktivitetResponse hentDialogerForAktivitet(HentDialogerForAktivitetRequest hentDialogerForAktivitetRequest) throws HentDialogerForAktivitetSikkerhetsbegrensning, HentDialogerForAktivitetUgyldigInput {
        throw new RuntimeException("not implemented");
    }

    @Override
    public HentDialogMedIdResponse hentDialogMedId(HentDialogMedIdRequest hentDialogMedIdRequest) throws HentDialogMedIdSikkerhetsbegrensning, HentDialogMedIdUgyldigInput {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void markerDialogSomLest(MarkerDialogSomLestRequest markerDialogSomLestRequest) throws MarkerDialogSomLestSikkerhetsbegrensning, MarkerDialogSomLestUgyldigInput {
        throw new RuntimeException("not implemented");
    }

    @Override
    public OpprettDialogForAktivitetResponse opprettDialogForAktivitet(OpprettDialogForAktivitetRequest opprettDialogForAktivitetRequest) throws OpprettDialogForAktivitetSikkerhetsbegrensning, OpprettDialogForAktivitetUgyldigInput {
        throw new RuntimeException("not implemented");
    }

    @Override
    public OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplan(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest) throws OpprettDialogForAktivitetsplanPersonIkkeFunnet, OpprettDialogForAktivitetsplanSikkerhetsbegrensning, OpprettDialogForAktivitetsplanUgyldigInput {
        String personIdent = opprettDialogForAktivitetsplanRequest.getPersonIdent();
        return of(opprettDialogForAktivitetsplanRequest)
                .map(soapServiceMapper::mapp)
                .map(dialogData -> appService.opprettDialogForAktivitetsplanPaIdent(dialogData, personIdent))
                .map(soapServiceMapper::maaaap)
                .get();
    }

    @Override
    public void opprettHenvendelseForDialog(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest) throws OpprettHenvendelseForDialogSikkerhetsbegrensning, OpprettHenvendelseForDialogUgyldigInput {
        of(opprettHenvendelseForDialogRequest)
                .map(soapServiceMapper::mapp2)
                .ifPresent(dialogData -> appService.opprettHenvendelseForDialog(dialogData));
    }

}

