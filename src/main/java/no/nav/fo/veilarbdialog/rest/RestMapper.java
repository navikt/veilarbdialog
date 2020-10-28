package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.AuthService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static no.nav.fo.veilarbdialog.service.AuthService.erEksternBruker;
import static no.nav.fo.veilarbdialog.service.AuthService.erInternBruker;

@Component
@RequiredArgsConstructor
class RestMapper {

    private final KontorsperreFilter kontorsperreFilter;
    private final AuthService auth;

    public DialogDTO somDialogDTO(DialogData dialogData) {

        List<HenvendelseData> henvendelser = dialogData
                .getHenvendelser()
                .stream()
                .filter((henvendelse) -> kontorsperreFilter.harTilgang(auth.getIdent().orElse(null), henvendelse.getKontorsperreEnhetId()))
                .collect(toList());

        Optional<HenvendelseData> sisteHenvendelse = henvendelser.stream()
                .max(comparing(HenvendelseData::getSendt));

        DialogDTO dto = new DialogDTO()
                .setId(Long.toString(dialogData.getId()))
                .setOverskrift(dialogData.getOverskrift())
                .setAktivitetId(dialogData.getAktivitetId())
                .setVenterPaSvar(dialogData.venterPaSvar())
                .setOpprettetDato(dialogData.getOpprettetDato())
                .setEgenskaper(dialogData.getEgenskaper()
                        .stream()
                        .map(egenskapType -> Egenskap.valueOf(egenskapType.name()))
                        .collect(Collectors.toList()))
                .setHistorisk(dialogData.isHistorisk())
                .setHenvendelser(henvendelser.stream()
                        .map(this::somHenvendelseDTO)
                        .collect(toList())
                )
                .setSisteDato(sisteHenvendelse
                        .map(HenvendelseData::getSendt)
                        .orElse(null)
                )
                .setSisteTekst(sisteHenvendelse
                        .map(HenvendelseData::getTekst)
                        .orElse(null));

        if (erEksternBruker()) {
            dto.setLest(dialogData.erLestAvBruker())
                    .setLestAvBrukerTidspunkt(null)
                    .setErLestAvBruker(false)
                    .setFerdigBehandlet(true);


        } else if (erInternBruker()) {
            dto.setLest(dialogData.erLestAvVeileder())
                    .setLestAvBrukerTidspunkt(dialogData.getLestAvBrukerTidspunkt())
                    .setErLestAvBruker(dialogData.erLestAvBruker())
                    .setFerdigBehandlet(dialogData.erFerdigbehandlet());
        }

        return dto;
    }

    private HenvendelseDTO somHenvendelseDTO(HenvendelseData henvendelseData) {
        HenvendelseDTO dto = new HenvendelseDTO()
                .setId(Long.toString(henvendelseData.id))
                .setDialogId(Long.toString(henvendelseData.dialogId))
                .setAvsender(BRUKER.equals(henvendelseData.avsenderType) ? Avsender.BRUKER : Avsender.VEILEDER)
                .setViktig(henvendelseData.viktig)
                .setSendt(henvendelseData.sendt)
                .setTekst(henvendelseData.tekst);

        if (erEksternBruker()) {
            dto.setLest(henvendelseData.lestAvBruker);
        } else if (erInternBruker()) {
            dto.setLest(henvendelseData.lestAvVeileder)
                    .setAvsenderId(henvendelseData.avsenderId);
        }
        return dto;
    }
}

