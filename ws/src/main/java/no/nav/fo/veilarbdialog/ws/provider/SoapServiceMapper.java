package no.nav.fo.veilarbdialog.ws.provider;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.util.DateUtils;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.informasjon.Dialog;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class SoapServiceMapper {

    public String brukerIdent(HentDialogerForBrukerRequest hentDialogerForBrukerRequest) {
        return hentDialogerForBrukerRequest.getPersonIdent();
    }

    public HentDialogerForBrukerResponse mapTil(List<DialogData> dialogData) {
        HentDialogerForBrukerResponse hentDialogerForBrukerResponse = new HentDialogerForBrukerResponse();
        dialogData.stream().map(this::maaaaap).forEach(hentDialogerForBrukerResponse.getDialogListe()::add);
        return hentDialogerForBrukerResponse;
    }

    private Dialog maaaaap(DialogData dialogData) {
        Dialog dialog = new Dialog();
        dialog.setTittel(dialogData.getOverskrift());
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

