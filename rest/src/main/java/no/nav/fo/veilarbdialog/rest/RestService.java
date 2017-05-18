package no.nav.fo.veilarbdialog.rest;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbdialog.api.DialogAktorController;
import no.nav.fo.veilarbdialog.api.DialogController;
import no.nav.fo.veilarbdialog.api.VeilederDialogController;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.service.AppService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.util.StringUtils.notNullOrEmpty;
import static no.nav.fo.veilarbdialog.util.StringUtils.of;


@Component
public class RestService implements DialogController, VeilederDialogController, DialogAktorController {

    @Inject
    private AppService appService;

    @Inject
    private RestMapper restMapper;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Inject
    private PepClient pepClient;

    @Override
    public List<DialogDTO> hentDialoger() {
        return appService.hentDialogerForBruker(getBrukerIdent())
                .stream()
                .map(restMapper::somDialogDTO)
                .collect(toList());
    }

    @Override
    public DialogDTO nyHenvendelse(NyHenvendelseDTO nyHenvendelseDTO) {
        long dialogId = finnDialogId(nyHenvendelseDTO);
        appService.opprettHenvendelseForDialog(HenvendelseData.builder()
                .dialogId(dialogId)
                .avsenderId(getVeilederIdent())
                .avsenderType(AvsenderType.VEILEDER)
                .tekst(nyHenvendelseDTO.tekst)
                .build()
        );
        return markerSomLest(dialogId);
    }

    private Long finnDialogId(NyHenvendelseDTO nyHenvendelseDTO) {
        if (notNullOrEmpty(nyHenvendelseDTO.dialogId)) {
            return Long.parseLong(nyHenvendelseDTO.dialogId);
        } else {
            return of(nyHenvendelseDTO.aktivitetId)
                    .flatMap(appService::hentDialogForAktivitetId)
                    .orElseGet(() -> opprettDialog(nyHenvendelseDTO))
                    .id;
        }
    }

    private DialogData opprettDialog(NyHenvendelseDTO nyHenvendelseDTO) {
        DialogData dialogData = DialogData.builder()
                .overskrift(nyHenvendelseDTO.overskrift)
                .aktorId(appService.hentAktoerIdForIdent(getBrukerIdent()))
                .aktivitetId(nyHenvendelseDTO.aktivitetId)
                .build();
        return appService.opprettDialogForAktivitetsplanPaIdent(dialogData);
    }

    @Override
    public DialogDTO markerSomLest(String dialogIdString) {
        return markerSomLest(Long.parseLong(dialogIdString));
    }

    private DialogDTO markerSomLest(long id) {
        return restMapper.somDialogDTO(appService.markerDialogSomLestAvVeileder(id));
    }

    private String getVeilederIdent() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private String getBrukerIdent() {
        return Optional.ofNullable(requestProvider.get().getParameter("fnr"))
                .map(pepClient::sjekkTilgangTilFnr)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    @Override
    public List<DialogAktorDTO> hentAktorerMedEndringerEtter(Date tidspunkt) {
        return appService.hentAktorerMedEndringerEtter(tidspunkt)
                .stream()
                .map(restMapper::somDTO)
                .collect(toList());
    }

    @Override
    public DialogDTO oppdaterDialog(OppdaterDialogDTO dialogDTO) {
        appService.oppdaterDialogStatus(DialogStatus.builder()
                .dialogId(Long.parseLong(dialogDTO.id))
                .venterPaSvar(dialogDTO.venterPaSvar)
                .ferdigbehandlet(dialogDTO.ferdigBehandlet)
                .build()
        );
        return markerSomLest(dialogDTO.id);
    }

}
