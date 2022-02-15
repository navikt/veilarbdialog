package no.nav.fo.veilarbdialog.internapi;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.veilarbdialog.internapi.model.Dialog;
import no.nav.veilarbdialog.internapi.model.Henvendelse;

import java.time.ZoneOffset;
import java.util.List;

public class InternDialogMapper {

    private InternDialogMapper() {}

    public static Dialog mapTilDialog(DialogData dialogData) {
        List<Henvendelse> henvendelser = dialogData.getHenvendelser()
                .stream()
                .map(InternDialogMapper::mapTilHenvendelse)
                .toList();

        return Dialog.builder()
                .aktivitetId(dialogData.getAktivitetId())
                .overskrift(dialogData.getOverskrift())
                .venterSvarNav(!dialogData.venterPaSvarFraBruker())
                .venterSvarBruker(dialogData.venterPaSvarFraBruker())
                .opprettetDato(dialogData.getOpprettetDato().toInstant().atOffset(ZoneOffset.UTC))
                .henvendelser(henvendelser)
                .build();
    }

    public static Henvendelse mapTilHenvendelse(HenvendelseData henvendelseData) {
        return Henvendelse.builder()
                .avsenderType(Henvendelse.AvsenderTypeEnum.valueOf(henvendelseData.getAvsenderType().name()))
                .sendtDato(henvendelseData.getSendt().toInstant().atOffset(ZoneOffset.UTC))
                .lestAvBruker(henvendelseData.isLestAvBruker())
                .lestAvVeileder(henvendelseData.isLestAvVeileder())
                .tekst(henvendelseData.getTekst())
                .build();
    }
}
