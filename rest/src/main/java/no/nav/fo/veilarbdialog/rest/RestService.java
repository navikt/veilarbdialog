package no.nav.fo.veilarbdialog.rest;

import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.fo.veilarbdialog.api.DialogController;
import no.nav.fo.veilarbdialog.api.VeilederDialogController;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.service.AutorisasjonService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.service.AutorisasjonService.erEksternBruker;
import static no.nav.fo.veilarbdialog.service.AutorisasjonService.erInternBruker;
import static no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker.nyDialogBruker;
import static no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker.nyDialogVeileder;
import static no.nav.fo.veilarbdialog.util.StringUtils.notNullOrEmpty;
import static no.nav.fo.veilarbdialog.util.StringUtils.of;

import no.nav.common.auth.SubjectHandler;


@Component
public class RestService implements DialogController, VeilederDialogController {

    @Inject
    private AppService appService;

    @Inject
    private RestMapper restMapper;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Inject
    private KontorsperreFilter kontorsperreFilter;

    @Inject
    private AutorisasjonService autorisasjonService;

    @Override
    public List<DialogDTO> hentDialoger() {
        return appService.hentDialogerForBruker(getBrukerIdent())
                .stream()
                .filter(dialog -> kontorsperreFilter.harTilgang(dialog.getKontorsperreEnhetId()))
                .map(restMapper::somDialogDTO)
                .collect(toList());
    }

    @Override
    public DialogDTO hentDialog(String dialogId) {
        return Optional.ofNullable(dialogId)
                .map(Long::parseLong)
                .map(appService::hentDialog)
                .map(restMapper::somDialogDTO)
                .orElseThrow(() -> new NotFoundException(""));
    }

    @Override
    public DialogDTO nyHenvendelse(NyHenvendelseDTO nyHenvendelseDTO) {
        long dialogId = finnDialogId(nyHenvendelseDTO);
        appService.opprettHenvendelseForDialog(HenvendelseData.builder()
                .dialogId(dialogId)
                .avsenderId(getLoggedInUserIdent())
                .avsenderType(erEksternBruker() ? AvsenderType.BRUKER : AvsenderType.VEILEDER)
                .tekst(nyHenvendelseDTO.tekst)
                .build()
        );
        DialogData dialogData = markerSomLest(dialogId);
        appService.updateDialogAktorFor(dialogData.getAktorId());
        return kontorsperreFilter.harTilgang(dialogData.getKontorsperreEnhetId()) ?
                restMapper.somDialogDTO(dialogData)
                : null;
    }

    private DialogData markerSomLest(long dialogId){
        if (erEksternBruker()){
            return appService.markerDialogSomLestAvBruker(dialogId);
        }
        return appService.markerDialogSomLestAvVeileder(dialogId);

    }

    @Override
    public DialogDTO forhandsorienteringPaAktivitet(NyHenvendelseDTO nyHenvendelseDTO) {
        autorisasjonService.skalVereInternBruker();
        long dialogId = finnDialogId(nyHenvendelseDTO);
        appService.updateDialogEgenskap(EgenskapType.PARAGRAF8, dialogId);
        appService.markerSomParagra8(dialogId);
        return nyHenvendelse(nyHenvendelseDTO.setEgenskaper(singletonList(Egenskap.PARAGRAF8)));
    }

    @Override
    public DialogDTO markerSomLest(String dialogId) {
        autorisasjonService.skalVereInternBruker();
        DialogData dialogData = appService.markerDialogSomLestAvVeileder(Long.parseLong(dialogId));
        return kontorsperreFilter.harTilgang(dialogData.getKontorsperreEnhetId()) ?
                restMapper.somDialogDTO(dialogData)
                : null;
    }

    @Override
    public DialogDTO oppdaterVenterPaSvar(String dialogId, boolean venter) {
        autorisasjonService.skalVereInternBruker();

        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .venterPaSvar(venter)
                .build();

        DialogData dialog = appService.oppdaterVentePaSvarTidspunkt(dialogStatus);
        appService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @Override
    public DialogDTO oppdaterFerdigbehandlet(String dialogId, boolean ferdigbehandlet) {
        autorisasjonService.skalVereInternBruker();

        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .ferdigbehandlet(ferdigbehandlet)
                .build();

        DialogData dialog = appService.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        appService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLest(dialogId);
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
                        .map(egenskap -> EgenskapType.valueOf(egenskap.name()))
                        .collect(Collectors.toList()))
                .build();
        DialogData opprettetDialog = appService.opprettDialogForAktivitetsplanPaIdent(dialogData);

        if (erEksternBruker()){
            nyDialogBruker(opprettetDialog);
        }
        else if (erInternBruker()){
            nyDialogVeileder(opprettetDialog);
        }


        return opprettetDialog;
    }

    private String getLoggedInUserIdent() {
        return SubjectHandler.getIdent().orElse(null);
    }

    private String getBrukerIdent() {
        if (erEksternBruker()) {
            return SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        }

        return of(requestProvider.get().getParameter("fnr")).orElseThrow(UgyldigRequest::new);
    }
}
