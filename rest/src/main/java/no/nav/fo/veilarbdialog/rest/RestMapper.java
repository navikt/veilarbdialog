package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Component
class RestMapper {

    public DialogDTO mapTilAktivitetDTO(DialogData dialogData) {
        return new DialogDTO()
                .setId(Long.toString(dialogData.getId()))
                .setOverskrift(dialogData.overskrift)
                .setHenvendelser(dialogData.henvendelser.stream()
                        .map(this::tilH)
                        .collect(toList())
                )
                .setSisteTekst(dialogData.henvendelser.stream()
                        .sorted(Comparator.comparing(HenvendelseData::getSendt))
                        .findFirst()
                        .map(HenvendelseData::getTekst)
                        .orElse(null))
                ;
    }

    private HenvendelseDTO tilH(HenvendelseData henvendelseData) {
        return new HenvendelseDTO()
                .setDialogId(Long.toString(henvendelseData.dialogId))
                .setSendt(henvendelseData.sendt)
                .setTekst(henvendelseData.tekst);
    }

}

