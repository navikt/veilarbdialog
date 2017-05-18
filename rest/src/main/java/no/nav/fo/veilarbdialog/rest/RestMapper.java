package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.domain.*;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static no.nav.fo.veilarbdialog.domain.LestAv.erDialogLestAvVeileder;
import static no.nav.fo.veilarbdialog.domain.LestAv.erHenvendelseLestAvVeileder;

@Component
class RestMapper {

    public DialogDTO somDialogDTO(DialogData dialogData) {
        List<HenvendelseData> henvendelser = dialogData.henvendelser;
        Optional<HenvendelseData> sisteHenvendelse = henvendelser.stream()
                .sorted(Comparator.comparing(HenvendelseData::getSendt).reversed())
                .findFirst();

        // TODO her gjøres endel masering av data som klienten kanskje burde gjøre selv. Eller?
        return new DialogDTO()
                .setId(Long.toString(dialogData.getId()))
                .setAktivitetId(dialogData.getAktivitetId())
                .setOverskrift(dialogData.overskrift)
                .setHenvendelser(henvendelser.stream()
                        .map(henvendelseData -> somHenvendelseDTO(henvendelseData, dialogData))
                        .collect(toList())
                )
                .setLest(erDialogLestAvVeileder(dialogData))
                .setSisteDato(sisteHenvendelse
                        .map(HenvendelseData::getSendt)
                        .orElse(null)
                )
                .setSisteTekst(sisteHenvendelse
                        .map(HenvendelseData::getTekst)
                        .orElse(null))
                ;
    }

    private HenvendelseDTO somHenvendelseDTO(HenvendelseData henvendelseData, DialogData dialogData) {
        return new HenvendelseDTO()
                .setDialogId(Long.toString(henvendelseData.dialogId))
                .setAvsender(henvendelseData.avsenderType == BRUKER ? Avsender.BRUKER : Avsender.VEILEDER)
                .setSendt(henvendelseData.sendt)
                .setLest(erHenvendelseLestAvVeileder(henvendelseData, dialogData))
                .setTekst(henvendelseData.tekst);
    }

}

