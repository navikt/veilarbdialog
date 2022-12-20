package no.nav.fo.veilarbdialog.internapi;

import no.nav.fo.veilarbdialog.domain.AktivitetId;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.veilarbdialog.internapi.model.Dialog;
import no.nav.veilarbdialog.internapi.model.Henvendelse;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class InternDialogMapper {

    private InternDialogMapper() {}

    public static Dialog mapTilDialog(DialogData dialogData) {
        List<Henvendelse> henvendelser = dialogData.getHenvendelser()
                .stream()
                .map(InternDialogMapper::mapTilHenvendelse)
                .toList();

        return Dialog.builder()
                .dialogId(Long.toString(dialogData.getId()))
                .aktivitetId(Optional.ofNullable(dialogData.getAktivitetId()).map(AktivitetId::getId).orElse(null))
                .oppfolgingsperiodeId(dialogData.getOppfolgingsperiode())
                .kontorsperreEnhetId(dialogData.getKontorsperreEnhetId())
                .overskrift(dialogData.getOverskrift())
                .venterSvarNav(dialogData.erUbehandlet())
                .venterSvarBruker(dialogData.venterPaSvarFraBruker())
                .opprettetDato(dialogData.getOpprettetDato().toInstant().atOffset(ZoneOffset.UTC))
                .henvendelser(henvendelser)
                .build();
    }

    public static Henvendelse mapTilHenvendelse(HenvendelseData henvendelseData) {
        return Henvendelse.builder()
                .dialogId(Long.toString(henvendelseData.getDialogId()))
                .kontorsperreEnhetId(henvendelseData.getKontorsperreEnhetId())
                .avsenderType(Henvendelse.AvsenderTypeEnum.valueOf(henvendelseData.getAvsenderType().name()))
                .avsenderId(henvendelseData.getAvsenderId())
                .sendtDato(henvendelseData.getSendt().toInstant().atOffset(ZoneOffset.UTC))
                .lestAvBruker(henvendelseData.isLestAvBruker())
                .lestAvVeileder(henvendelseData.isLestAvVeileder())
                .tekst(henvendelseData.getTekst())
                .build();
    }
}
