package no.nav.fo.veilarbdialog.rest;

import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbdialog.api.DialogController;
import no.nav.fo.veilarbdialog.api.VeilederDialogController;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.AppService;

import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.util.FunkjsonelleMetrikker.nyDialogVeilederMetrikk;
import static no.nav.fo.veilarbdialog.util.FunkjsonelleMetrikker.nyHenvedelseVeilederMetrikk;
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

    @Inject
    private KontorsperreFilter kontorsperreFilter;

    @Override
    public List<DialogDTO> hentDialoger() {        
        return hentDialogerForBruker(getBrukerIdent());
    }

    @Override
    public List<DialogDTO> hentDialogerForBruker(String fnr) {
        return appService.hentDialogerForBruker(fnr)
                .stream()
                .filter(dialog -> kontorsperreFilter.harTilgang(dialog.getKontorsperreEnhetId()))
                .map(restMapper::somDialogDTO)
                .collect(toList());
    }

    @Override
    public DialogDTO nyHenvendelse(NyHenvendelseDTO nyHenvendelseDTO) {
        return nyHenvendelseForBruker(getBrukerIdent(), nyHenvendelseDTO);
    }

    @Override
    public DialogDTO nyHenvendelseForBruker(String fnr, NyHenvendelseDTO nyHenvendelseDTO) {
        long dialogId = finnDialogId(fnr, nyHenvendelseDTO);
        appService.opprettHenvendelseForDialog(HenvendelseData.builder()
                .dialogId(dialogId)
                .avsenderId(getVeilederIdent())
                .avsenderType(AvsenderType.VEILEDER)
                .tekst(nyHenvendelseDTO.tekst)
                .build()
        );
        DialogData dialogData = appService.markerDialogSomLestAvVeileder(dialogId);
        appService.updateDialogAktorFor(dialogData.getAktorId());
        nyHenvedelseVeilederMetrikk(dialogData);
        return kontorsperreFilter.harTilgang(dialogData.getKontorsperreEnhetId()) ? 
                restMapper.somDialogDTO(dialogData) 
                : null;
    }

    @Override
    public DialogDTO markerSomLest(String dialogId) {

        DialogData dialogData = appService.markerDialogSomLestAvVeileder(Long.parseLong(dialogId));
        return kontorsperreFilter.harTilgang(dialogData.getKontorsperreEnhetId()) ? 
                restMapper.somDialogDTO(dialogData) 
                : null;
    }

    @Override
    public DialogDTO oppdaterVenterPaSvar(String dialogId, boolean venter) {
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
        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .ferdigbehandlet(ferdigbehandlet)
                .build();

        DialogData dialog = appService.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        appService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    private Long finnDialogId(String fnr, NyHenvendelseDTO nyHenvendelseDTO) {
        if (notNullOrEmpty(nyHenvendelseDTO.dialogId)) {
            return Long.parseLong(nyHenvendelseDTO.dialogId);
        } else {
            return of(nyHenvendelseDTO.aktivitetId)
                    .flatMap(appService::hentDialogForAktivitetId)
                    .orElseGet(() -> opprettDialog(fnr, nyHenvendelseDTO))
                    .getId();
        }
    }

    private DialogData opprettDialog(String fnr, NyHenvendelseDTO nyHenvendelseDTO) {
        DialogData dialogData = DialogData.builder()
                .overskrift(nyHenvendelseDTO.overskrift)
                .aktorId(appService.hentAktoerIdForIdent(fnr))
                .aktivitetId(nyHenvendelseDTO.aktivitetId)
                .egenskaper(nyHenvendelseDTO.egenskaper
                        .stream()
                        .map(egenskap -> EgenskapType.ESKALERINGSVARSEL)
                        .collect(Collectors.toList()))
                .build();
        DialogData opprettetDialog = appService.opprettDialogForAktivitetsplanPaIdent(dialogData);
        nyDialogVeilederMetrikk(opprettetDialog);
        return opprettetDialog;
    }

    private String getVeilederIdent() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private String getBrukerIdent() {
        return of(requestProvider.get().getParameter("fnr")).orElseThrow(UgyldigRequest::new);
    }

}
