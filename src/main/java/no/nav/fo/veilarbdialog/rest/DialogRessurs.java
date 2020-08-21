package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.service.AutorisasjonService;
import no.nav.fo.veilarbdialog.service.KladdService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.service.AutorisasjonService.erEksternBruker;
import static no.nav.fo.veilarbdialog.service.AutorisasjonService.erInternBruker;
import static no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker.nyDialogBruker;
import static no.nav.fo.veilarbdialog.util.FunksjonelleMetrikker.nyDialogVeileder;

@RestController
@RequestMapping("dialog")
@Import({
        RestMapper.class,
        KontorsperreFilter.class
})
@RequiredArgsConstructor
public class DialogRessurs {

    private final AppService appService;
    private final RestMapper restMapper;
    private final ServiceLoader.Provider<HttpServletRequest> requestProvider;
    private final KontorsperreFilter kontorsperreFilter;
    private final AutorisasjonService autorisasjonService;
    private final KladdService kladdService;

    @GetMapping
    public List<DialogDTO> hentDialoger() {
        return appService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(dialog -> kontorsperreFilter.harTilgang(getLoggedInUserIdent(), dialog.getKontorsperreEnhetId()))
                .map(restMapper::somDialogDTO)
                .collect(toList());
    }

    @GetMapping("sistOppdatert")
    public SistOppdatert sistOppdatert() {
        Date oppdatert = appService.hentSistOppdatertForBruker(getContextUserIdent(), getLoggedInUserIdent());
        return new SistOppdatert(oppdatert);
    }

    @GetMapping("antallUleste")
    public AntallUlesteDTO antallUleste() {
        long antall = appService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(erEksternBruker() ? DialogData::erUlestForBruker : DialogData::erUlestAvVeileder)
                .filter(it -> !it.isHistorisk())
                .count();

        return new AntallUlesteDTO(toIntExact(antall));
    }

    @GetMapping("{dialogId}")
    public DialogDTO hentDialog(@PathVariable String dialogId) {
        return Optional.ofNullable(dialogId)
                .map(Long::parseLong)
                .map(appService::hentDialog)
                .map(restMapper::somDialogDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public DialogDTO nyHenvendelse(NyHenvendelseDTO nyHenvendelseDTO) {
        slettUtdattertKladd(nyHenvendelseDTO);

        long dialogId = finnDialogId(nyHenvendelseDTO);
        appService.opprettHenvendelseForDialog(HenvendelseData.builder()
                .dialogId(dialogId)
                .avsenderId(getLoggedInUserIdent())
                .viktig(!nyHenvendelseDTO.egenskaper.isEmpty())
                .avsenderType(erEksternBruker() ? AvsenderType.BRUKER : AvsenderType.VEILEDER)
                .tekst(nyHenvendelseDTO.tekst)
                .build()
        );
        DialogData dialogData = markerSomLest(dialogId);
        appService.updateDialogAktorFor(dialogData.getAktorId());
        return kontorsperreFilter.harTilgang(getLoggedInUserIdent(), dialogData.getKontorsperreEnhetId()) ?
                restMapper.somDialogDTO(dialogData)
                : null;
    }

    private void slettUtdattertKladd(NyHenvendelseDTO nyHenvendelseDTO) {
        Person person = getContextUserIdent();
        if (person instanceof Person.Fnr) {
            kladdService.deleteKladd(person.get(), nyHenvendelseDTO.dialogId, nyHenvendelseDTO.aktivitetId);
        }
    }

    @PutMapping("{dialogId}/les")
    public DialogDTO markerSomLest(@PathVariable String dialogId) {
        DialogData dialogData = markerSomLest(Long.parseLong(dialogId));
        return kontorsperreFilter.harTilgang(getLoggedInUserIdent(), dialogData.getKontorsperreEnhetId()) ?
                restMapper.somDialogDTO(dialogData)
                : null;
    }

    private DialogData markerSomLest(long dialogId) {
        if (erEksternBruker()) {
            return appService.markerDialogSomLestAvBruker(dialogId);
        }
        return appService.markerDialogSomLestAvVeileder(dialogId);

    }

    @PutMapping("{dialogId}/venter_pa_svar/{venter}")
    public DialogDTO oppdaterVenterPaSvar(@PathVariable String dialogId, @PathVariable boolean venter) {
        autorisasjonService.skalVereInternBruker();

        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .venterPaSvar(venter)
                .build();

        DialogData dialog = appService.oppdaterVentePaSvarTidspunkt(dialogStatus);
        appService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @PutMapping("{dialogId}/ferdigbehandlet/{ferdigbehandlet}")
    public DialogDTO oppdaterFerdigbehandlet(@PathVariable String dialogId, @PathVariable boolean ferdigbehandlet) {
        autorisasjonService.skalVereInternBruker();

        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .ferdigbehandlet(ferdigbehandlet)
                .build();

        DialogData dialog = appService.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        appService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @PostMapping("forhandsorientering")
    public DialogDTO forhandsorienteringPaAktivitet(NyHenvendelseDTO nyHenvendelseDTO) {
        autorisasjonService.skalVereInternBruker();

        long dialogId = finnDialogId(nyHenvendelseDTO);
        appService.updateDialogEgenskap(EgenskapType.PARAGRAF8, dialogId);
        appService.markerSomParagra8(dialogId);
        return nyHenvendelse(nyHenvendelseDTO.setEgenskaper(singletonList(Egenskap.PARAGRAF8)));
    }

    private Long finnDialogId(NyHenvendelseDTO nyHenvendelseDTO) {
        if (StringUtils.isEmpty(nyHenvendelseDTO.dialogId)) {
            return Long.parseLong(nyHenvendelseDTO.dialogId);
        } else {
            return Optional.ofNullable(nyHenvendelseDTO.aktivitetId)
                    .filter(StringUtils::isNotEmpty)
                    .flatMap(appService::hentDialogForAktivitetId)
                    .orElseGet(() -> opprettDialog(nyHenvendelseDTO))
                    .getId();
        }
    }

    private DialogData opprettDialog(NyHenvendelseDTO nyHenvendelseDTO) {
        DialogData dialogData = DialogData.builder()
                .overskrift(nyHenvendelseDTO.overskrift)
                .aktorId(appService.hentAktoerIdForPerson(getContextUserIdent()))
                .aktivitetId(nyHenvendelseDTO.aktivitetId)
                .egenskaper(nyHenvendelseDTO.egenskaper
                        .stream()
                        .map(egenskap -> EgenskapType.valueOf(egenskap.name()))
                        .collect(Collectors.toList()))
                .build();
        DialogData opprettetDialog = appService.opprettDialogForAktivitetsplanPaIdent(dialogData);

        if (erEksternBruker()) {
            nyDialogBruker(opprettetDialog);
        } else if (erInternBruker()) {
            nyDialogVeileder(opprettetDialog);
        }


        return opprettetDialog;
    }

    private static String getLoggedInUserIdent() {
        return SubjectHandler.getIdent().orElse(null);
    }

    private Person getContextUserIdent() {
        if (erEksternBruker()) {
            return SubjectHandler.getIdent().map(Person::fnr).orElseThrow(RuntimeException::new);
        }

        Optional<Person> fnr = Optional.ofNullable(requestProvider.get().getParameter("fnr")).map(Person::fnr);
        Optional<Person> aktorId = Optional.ofNullable(requestProvider.get().getParameter("aktorId")).map(Person::aktorId);
        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new));
    }
}
