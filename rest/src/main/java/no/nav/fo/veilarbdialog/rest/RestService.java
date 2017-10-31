package no.nav.fo.veilarbdialog.rest;

import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbdialog.api.DialogController;
import no.nav.fo.veilarbdialog.api.VeilederDialogController;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.service.AppService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.util.StringUtils.notNullOrEmpty;
import static no.nav.fo.veilarbdialog.util.StringUtils.of;


@Component
public class RestService implements DialogController, VeilederDialogController {

    @Inject
    private AppService appService;

    @Inject
    private RestMapper restMapper;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

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
        DialogData dialogData = appService.markerDialogSomLestAvVeileder(dialogId);
        appService.updateDialogAktorFor(dialogData.getAktorId());
        return restMapper.somDialogDTO(dialogData);
    }

    private Long finnDialogId(NyHenvendelseDTO nyHenvendelseDTO) {
        if (notNullOrEmpty(nyHenvendelseDTO.dialogId)) {
            return Long.parseLong(nyHenvendelseDTO.dialogId);
        } else {
            return of(nyHenvendelseDTO.aktivitetId)
                    .flatMap(appService::hentDialogForAktivitetId)
                    .orElseGet(() -> opprettDialog(nyHenvendelseDTO))
                    .getId();
        }
    }

    private DialogData opprettDialog(NyHenvendelseDTO nyHenvendelseDTO) {
        DialogData dialogData = DialogData.builder()
                .overskrift(nyHenvendelseDTO.overskrift)
                .aktorId(appService.hentAktoerIdForIdent(getBrukerIdent()))
                .aktivitetId(nyHenvendelseDTO.aktivitetId)
                .egenskaper(nyHenvendelseDTO.egenskaper
                        .stream()
                        .map(egenskap -> EgenskapType.ESKALERINGSVARSEL)
                        .collect(Collectors.toList()))
                .build();
        return appService.opprettDialogForAktivitetsplanPaIdent(dialogData);
    }

    @Override
    public DialogDTO markerSomLest(String dialogId) {
        DialogData dialogData = appService.markerDialogSomLestAvVeileder(Long.parseLong(dialogId));
        appService.updateDialogAktorFor(dialogData.getAktorId());
        return restMapper.somDialogDTO(dialogData);
    }

    private String getVeilederIdent() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private String getBrukerIdent() {
        return of(requestProvider.get().getParameter("fnr")).orElseThrow(UgyldigRequest::new);
    }

    @Override
    public DialogDTO oppdaterVenterPaSvar(String dialogId, boolean venter) {
        return oppdaterStatus(dialogId, dialogStatusBuilder -> appService.oppdaterVentePaSvarTidspunkt(dialogStatusBuilder.venterPaSvar(venter).build()));
    }

    @Override
    public DialogDTO oppdaterFerdigbehandlet(String dialogId, boolean ferdigbehandlet) {
        return oppdaterStatus(dialogId, b -> appService.oppdaterFerdigbehandletTidspunkt(b.ferdigbehandlet(ferdigbehandlet).build()));
    }

    private DialogDTO oppdaterStatus(String dialogId, Consumer<DialogStatus.DialogStatusBuilder> dialogStatusBuilderConsumer) {
        dialogStatusBuilderConsumer.accept(DialogStatus.builder().dialogId(Long.parseLong(dialogId)));
        return markerSomLest(dialogId);
    }

}
