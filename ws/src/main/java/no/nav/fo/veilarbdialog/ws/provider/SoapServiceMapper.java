package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.DateUtils;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Bruker;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Dialog;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbdialog.util.DateUtils.xmlCalendar;

@Component
class SoapServiceMapper {

    public Dialog somWSDialog(DialogData dialogData, String personIdent) {
        Bruker bruker = new Bruker();
        bruker.setPersonIdent(personIdent);

        Dialog dialog = new Dialog();
        dialog.setId(Long.toString(dialogData.id));
        dialog.setTittel(dialogData.overskrift);
        dialog.setGjelder(bruker);
        dialog.setKontekstId(Long.toString(dialogData.id)); // TODO midlertidig, tilfredstill wsdl-constraint
        dialog.setOpprettet(dialogData.getHenvendelser()
                .stream()
                .map(HenvendelseData::getSendt)
                .sorted()
                .map(DateUtils::xmlCalendar)
                .findFirst()
                .orElseGet(()-> xmlCalendar(new Date())) // TODO eller skal det inn timestamp på dialogen?
        );
        return dialog;
    }

    public DialogData somDialogData(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest) {
        return DialogData.builder()
                .overskrift(opprettDialogForAktivitetsplanRequest.getTittel())
                .build();
    }

    public HenvendelseData somHenvendelseData(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest) {
        return HenvendelseData.builder()
                .dialogId(Long.parseLong(opprettHenvendelseForDialogRequest.getDialogId()))
                .tekst(opprettHenvendelseForDialogRequest.getTekst())
                .build();
    }

}

