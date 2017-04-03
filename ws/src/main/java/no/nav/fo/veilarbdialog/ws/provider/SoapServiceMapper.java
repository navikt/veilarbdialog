package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.DateUtils;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Bruker;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Dialog;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
class SoapServiceMapper {

    public HentDialogerForBrukerResponse mapTil(List<DialogData> dialogData, String personIdent) {
        HentDialogerForBrukerResponse hentDialogerForBrukerResponse = new HentDialogerForBrukerResponse();
        dialogData.stream().map((DialogData dialogData1) -> maaaaap(dialogData1,personIdent)).forEach(hentDialogerForBrukerResponse.getDialogListe()::add);
        return hentDialogerForBrukerResponse;
    }

    public Dialog maaaaap(DialogData dialogData, String personIdent) {
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
                .orElse(null)
        );
        return dialog;
    }

    public DialogData mapp(OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest) {
        return DialogData.builder()
                .overskrift(opprettDialogForAktivitetsplanRequest.getTittel())
                .build();
    }

    public OpprettDialogForAktivitetsplanResponse maaaap(DialogData dialogData) {
        OpprettDialogForAktivitetsplanResponse opprettDialogForAktivitetsplanResponse = new OpprettDialogForAktivitetsplanResponse();
        opprettDialogForAktivitetsplanResponse.setDialogId(Long.toString(dialogData.id));
        return opprettDialogForAktivitetsplanResponse;
    }

    public HenvendelseData mapp2(OpprettHenvendelseForDialogRequest opprettHenvendelseForDialogRequest) {
        return HenvendelseData.builder()
                .dialogId(Long.parseLong(opprettHenvendelseForDialogRequest.getDialogId()))
                .tekst(opprettHenvendelseForDialogRequest.getTekst())
                .build();
    }
}

