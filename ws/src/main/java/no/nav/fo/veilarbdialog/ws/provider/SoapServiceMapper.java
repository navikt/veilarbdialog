package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.EgenskapType;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.util.DateUtils;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.OpprettDialogForAktivitetRequest;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.OpprettDialogForAktivitetsplanRequest;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.OpprettHenvendelseForDialogRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static no.nav.fo.veilarbdialog.util.DateUtils.xmlCalendar;

@Component
class SoapServiceMapper {

    @Inject
    private AppService appService;

    public Dialog somWSDialog(DialogData dialogData, String personIdent) {
        List<HenvendelseData> henvendelser = dialogData.getHenvendelser();

        Bruker bruker = new Bruker();
        bruker.setPersonIdent(personIdent);

        Dialog dialog = new Dialog();
        dialog.setId(Long.toString(dialogData.getId()));
        dialog.setTittel(dialogData.getOverskrift());
        dialog.setErLest(dialogData.erLestAvBruker());
        dialog.setGjelder(bruker);
        dialog.setKontekstId(ofNullable(dialogData.getAktivitetId()).orElse(""));
        dialog.setOpprettet(henvendelser
                .stream()
                .map(HenvendelseData::getSendt)
                .sorted()
                .map(DateUtils::xmlCalendar)
                .findFirst()
                .orElseGet(() -> xmlCalendar(new Date())) // TODO eller skal det inn timestamp på dialogen?
        );
        dialog.setErBehandlet(dialogData.erFerdigbehandlet());
        dialog.setErBesvart(!dialogData.venterPaSvar());
        dialog.setErHistorisk(dialogData.isHistorisk());
        dialogData.getEgenskaper().forEach(egenskap -> dialog.getEgenskaper().add(Egenskap.ESKALERINGSVARSEL) );
        henvendelser.stream().map(h -> somHenvendelse(h, dialogData, personIdent)).forEach(dialog.getHenvendelseListe()::add);
        return dialog;
    }

    private Henvendelse somHenvendelse(HenvendelseData henvendelseData, DialogData dialogData, String personIdent) {
        Henvendelse henvendelse = new Henvendelse();
        henvendelse.setId("");  // TODO midlertidig, tilfredstill wsdl-constraint
        henvendelse.setTekst(henvendelseData.tekst);
        henvendelse.setSendt(DateUtils.xmlCalendar(henvendelseData.sendt));
        henvendelse.setAvsender(finnAktor(henvendelseData, dialogData, personIdent));
        henvendelse.setLest(henvendelseData.lestAvBruker);
        return henvendelse;
    }

    private Aktoer finnAktor(HenvendelseData henvendelseData, DialogData dialogData, String personIdent) {
        switch (henvendelseData.avsenderType) {
            case BRUKER:
                Bruker bruker = new Bruker();
                bruker.setPersonIdent(personIdent);
                return bruker;
            case VEILEDER:
                Veileder veileder = new Veileder();
                veileder.setPersonIdent(henvendelseData.avsenderId);
                return veileder;
            default:
                throw new IllegalStateException();
        }
    }

    public DialogData somDialogData(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest) {
        return DialogData.builder()
                .overskrift(opprettDialogForAktivitetsplanRequest.getTittel())
                .aktorId(appService.hentAktoerIdForIdent(opprettDialogForAktivitetsplanRequest.getPersonIdent()))
                .egenskaper(opprettDialogForAktivitetsplanRequest
                        .getEgenskaper()
                        .stream()
                        .map(tmp -> EgenskapType.ESKALERINGSVARSEL)
                        .collect(Collectors.toList()))
                .build();
    }

    public DialogData somDialogData(OpprettDialogForAktivitetRequest opprettDialogForAktivitetRequest, String personIdent) {
        return DialogData.builder()
                .overskrift(opprettDialogForAktivitetRequest.getTittel())
                .aktorId(appService.hentAktoerIdForIdent(personIdent))
                .aktivitetId(opprettDialogForAktivitetRequest.getAktivitetId())
                .egenskaper(opprettDialogForAktivitetRequest
                        .getEgenskaper()
                        .stream()
                        .map(tmp -> EgenskapType.ESKALERINGSVARSEL)
                        .collect(Collectors.toList()))
                .build();
    }

    public HenvendelseData somHenvendelseData(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest, String personIdent) {
        return HenvendelseData.builder()
                .dialogId(Long.parseLong(opprettHenvendelseForDialogRequest.getDialogId()))
                .tekst(opprettHenvendelseForDialogRequest.getTekst())
                .avsenderId(appService.hentAktoerIdForIdent(personIdent))
                .avsenderType(BRUKER)
                .build();
    }

}

