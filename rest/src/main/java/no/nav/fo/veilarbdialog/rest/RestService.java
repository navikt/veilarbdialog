package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbdialog.api.DialogController;
import no.nav.fo.veilarbdialog.api.VeilederDialogController;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.metrics.MetricsFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
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
        nyHenvedelseMetrikker(nyHenvendelseDTO);
        return restMapper.somDialogDTO(dialogData);
    }

    private void nyHenvedelseMetrikker(NyHenvendelseDTO nyHenvendelseDTO) {
        MetricsFactory
                .createEvent("NyMeldingVeileder")
                .addFieldToReport("paaAktivitet", notNullOrEmpty(nyHenvendelseDTO.aktivitetId))
                .setSuccess()
                .report();
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
        DialogData opprettetDialog = appService.opprettDialogForAktivitetsplanPaIdent(dialogData);
        nyDialogMetrik(nyHenvendelseDTO);
        return opprettetDialog;
    }

    private void nyDialogMetrik(NyHenvendelseDTO nyHenvendelseDTO) {
        MetricsFactory
                .createEvent("NyDialogVeileder")
                .addFieldToReport("paaAktivitet", notNullOrEmpty(nyHenvendelseDTO.aktivitetId))
                .setSuccess()
                .report();
    }

    @Override
    public DialogDTO markerSomLest(String dialogId) {
        DialogDTO dialogDTO = markerSomLestUtenMetrikker(dialogId);
        MetricsFactory.createEvent("MarkerSomLestVeileder").setSuccess().report();
        return dialogDTO;
    }

    private DialogDTO markerSomLestUtenMetrikker(String dialogId) {
        DialogData dialogData = appService.markerDialogSomLestAvVeileder(Long.parseLong(dialogId));
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
        val dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .venterPaSvar(venter)
                .build();

        val dialog = appService.oppdaterVentePaSvarTidspunkt(dialogStatus);
        appService.updateDialogAktorFor(dialog.getAktorId());

        MetricsFactory
                .createEvent("oppdaterVenterPaSvar")
                .addFieldToReport("venter", venter)
                .setSuccess()
                .report();

        return markerSomLestUtenMetrikker(dialogId);
    }

    @Override
    public DialogDTO oppdaterFerdigbehandlet(String dialogId, boolean ferdigbehandlet) {
        val dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .ferdigbehandlet(ferdigbehandlet)
                .build();

        val dialog = appService.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        appService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLestUtenMetrikker(dialogId);
    }
}
