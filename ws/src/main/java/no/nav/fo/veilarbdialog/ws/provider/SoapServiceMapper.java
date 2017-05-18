package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
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

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbdialog.util.DateUtils.xmlCalendar;

@Component
class SoapServiceMapper {

    @Inject
    private AppService appService;

    public Dialog somWSDialog(DialogData dialogData, String personIdent) {
        List<HenvendelseData> henvendelser = dialogData.henvendelser;

        Bruker bruker = new Bruker();
        bruker.setPersonIdent(personIdent);

        Dialog dialog = new Dialog();
        dialog.setId(Long.toString(dialogData.id));
        dialog.setTittel(dialogData.overskrift);
        dialog.setErLest(dialogData.lestAvBruker);
        dialog.setGjelder(bruker);
        dialog.setKontekstId(ofNullable(dialogData.aktivitetId).orElse(""));
        dialog.setOpprettet(henvendelser
                .stream()
                .map(HenvendelseData::getSendt)
                .sorted()
                .map(DateUtils::xmlCalendar)
                .findFirst()
                .orElseGet(() -> xmlCalendar(new Date())) // TODO eller skal det inn timestamp på dialogen?
        );
        dialog.setErBehandlet(dialogData.ferdigbehandlet);
        dialog.setErBesvart(!dialogData.venterPaSvar);
        henvendelser.stream().map(h -> somHenvendelse(h, personIdent)).forEach(dialog.getHenvendelseListe()::add);
        return dialog;
    }

    private Henvendelse somHenvendelse(HenvendelseData henvendelseData, String personIdent) {
        Henvendelse henvendelse = new Henvendelse();
        henvendelse.setId("");  // TODO midlertidig, tilfredstill wsdl-constraint
        henvendelse.setTekst(henvendelseData.tekst);
        henvendelse.setSendt(DateUtils.xmlCalendar(henvendelseData.sendt));
        henvendelse.setAvsender(finnAktor(henvendelseData, personIdent));
        henvendelse.setLest(henvendelseData.lestAvBruker);
        return henvendelse;
    }

    private Aktoer finnAktor(HenvendelseData henvendelseData, String personIdent) {
        if (personIdent.equals(henvendelseData.avsenderId)) {
            Bruker bruker = new Bruker();
            bruker.setPersonIdent(personIdent);
            return bruker;
        } else {
            Veileder veileder = new Veileder();
            veileder.setPersonIdent(henvendelseData.avsenderId);
            return veileder;
        }
    }

    public DialogData somDialogData(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest) {
        return DialogData.builder()
                .overskrift(opprettDialogForAktivitetsplanRequest.getTittel())
                .aktorId(appService.hentAktoerIdForIdent(opprettDialogForAktivitetsplanRequest.getPersonIdent()))
                .build();
    }

    public DialogData somDialogData(OpprettDialogForAktivitetRequest opprettDialogForAktivitetRequest, String personIdent) {
        return DialogData.builder()
                .overskrift(opprettDialogForAktivitetRequest.getTittel())
                .aktorId(appService.hentAktoerIdForIdent(personIdent))
                .aktivitetId(opprettDialogForAktivitetRequest.getAktivitetId())
                .build();
    }

    public HenvendelseData somHenvendelseData(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest, String personIdent) {
        return HenvendelseData.builder()
                .dialogId(Long.parseLong(opprettHenvendelseForDialogRequest.getDialogId()))
                .tekst(opprettHenvendelseForDialogRequest.getTekst())
                .avsenderId(appService.hentAktoerIdForIdent(personIdent))
                .avsenderType(AvsenderType.BRUKER)
                .build();
    }

}

