package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.api.DialogController;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.domain.NyDialogDTO;
import no.nav.fo.veilarbdialog.service.AppService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;


@Component
public class RestService implements DialogController {

    @Inject
    private AppService appService;

    @Inject
    private RestMapper restMapper;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Override
    public List<DialogDTO> hentDialogerForBruker() {
        return appService.hentDialogerForBruker(getUserIdent())
                .stream()
                .map(restMapper::somAktivitetDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DialogDTO opprettDialogForAktivitetsplan(NyDialogDTO dialogDTO) {
        return of(DialogData.builder().overskrift(dialogDTO.overskrift).build())
                .map(dialogData -> appService.opprettDialogForAktivitetsplanPaIdent(dialogData, getUserIdent()))
                .map(dialogData -> appService.opprettHenvendelseForDialog(HenvendelseData.builder()
                        .dialogId(dialogData.id)
                        .tekst(dialogDTO.tekst)
                        .build())
                )
                .map(restMapper::somAktivitetDTO)
                .get();
    }

    private String getUserIdent() {
        return Optional.ofNullable(requestProvider.get().getParameter("fnr"))
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

}
