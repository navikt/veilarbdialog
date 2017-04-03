package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.ws.consumer.AktoerConsumer;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.binding.*;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

import java.util.List;
import java.util.Optional;

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
        String brukerIdent = hentDialogerForBrukerRequest.getPersonIdent();
        return of(brukerIdent)
                .map(appService::hentDialogerForBruker)
                .map(dialogData -> soapServiceMapper.mapTil(dialogData, brukerIdent))
                .get();
    }

    @Override
    public HentDialogerForAktivitetResponse hentDialogerForAktivitet(HentDialogerForAktivitetRequest hentDialogerForAktivitetRequest) throws HentDialogerForAktivitetSikkerhetsbegrensning, HentDialogerForAktivitetUgyldigInput {
        throw new RuntimeException("not implemented");
    }

    @Override
    public HentDialogMedIdResponse hentDialogMedId(HentDialogMedIdRequest hentDialogMedIdRequest) throws HentDialogMedIdSikkerhetsbegrensning, HentDialogMedIdUgyldigInput {
        String personIdent = SubjectHandler.getSubjectHandler().getUid();
        HentDialogMedIdResponse hentDialogMedIdResponse = new HentDialogMedIdResponse();
        hentDialogMedIdResponse.getDialogListe().add(soapServiceMapper.maaaaap(appService.hentDialog(Long.parseLong(hentDialogMedIdRequest.getDialogId())), personIdent));
        return hentDialogMedIdResponse;
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

