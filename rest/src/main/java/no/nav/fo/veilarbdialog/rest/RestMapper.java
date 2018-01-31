package no.nav.fo.veilarbdialog.rest;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import no.nav.fo.veilarbdialog.domain.Avsender;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.Egenskap;
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;

@Component
class RestMapper {

    @Inject
    private KontorsperreFilter kontorsperreFilter;
    
    public DialogDTO somDialogDTO(DialogData dialogData) {

        List<HenvendelseData> henvendelser = dialogData
                .getHenvendelser()
                .stream()
                .filter((henvendelse) -> kontorsperreFilter.harTilgang(henvendelse.getKontorsperreEnhetId()))
                .collect(toList());
        
        Optional<HenvendelseData> sisteHenvendelse = henvendelser.stream()
                .sorted(comparing(HenvendelseData::getSendt).reversed())
                .findFirst();

        return new DialogDTO()
                .setId(Long.toString(dialogData.getId()))
                .setAktivitetId(dialogData.getAktivitetId())
                .setOverskrift(dialogData.getOverskrift())
                .setHenvendelser(henvendelser.stream()
                        .map(this::somHenvendelseDTO)
                        .collect(toList())
                )
                .setLest(dialogData.erLestAvVeileder())
                .setLestAvBrukerTidspunkt(dialogData.getLestAvBrukerTidspunkt())
                .setErLestAvBruker(dialogData.erLestAvBruker())
                .setFerdigBehandlet(dialogData.erFerdigbehandlet())
                .setVenterPaSvar(dialogData.venterPaSvar())
                .setSisteDato(sisteHenvendelse
                        .map(HenvendelseData::getSendt)
                        .orElse(null)
                )
                .setOpprettetDato(dialogData.getOpprettetDato())
                .setEgenskaper(dialogData.getEgenskaper()
                        .stream()
                        .map(tmp -> Egenskap.ESKALERINGSVARSEL)
                        .collect(Collectors.toList()))
                .setHistorisk(dialogData.isHistorisk())
                .setSisteTekst(sisteHenvendelse
                        .map(HenvendelseData::getTekst)
                        .orElse(null))
                ;
    }

    private HenvendelseDTO somHenvendelseDTO(HenvendelseData henvendelseData) {
        return new HenvendelseDTO()
                .setId(Long.toString(henvendelseData.id))
                .setDialogId(Long.toString(henvendelseData.dialogId))
                .setAvsender(henvendelseData.avsenderType == BRUKER ? Avsender.BRUKER : Avsender.VEILEDER)
                .setAvsenderId(henvendelseData.avsenderId)
                .setSendt(henvendelseData.sendt)
                .setLest(henvendelseData.lestAvVeileder)
                .setTekst(henvendelseData.tekst);
    }
}

