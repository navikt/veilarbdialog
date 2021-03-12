package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.metrics.FunksjonelleMetrikker;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.fo.veilarbdialog.service.KladdService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Transactional
@RestController
@RequestMapping(
        value = "/api/dialog",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class DialogRessurs {

    private final DialogDataService dialogDataService;
    private final RestMapper restMapper;
    private final HttpServletRequest httpServletRequest;
    private final KontorsperreFilter kontorsperreFilter;
    private final AuthService auth;
    private final KladdService kladdService;
    private final FunksjonelleMetrikker funksjonelleMetrikker;

    @GetMapping
    public List<DialogDTO> hentDialoger() {
        return dialogDataService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(kontorsperreFilter::filterKontorsperre)
                .map(restMapper::somDialogDTO)
                .collect(toList());
    }

    @GetMapping("sistOppdatert")
    public SistOppdatert sistOppdatert() {

        Date oppdatert = dialogDataService.hentSistOppdatertForBruker(getContextUserIdent(), auth.getIdent().orElse(null)); // TODO: This orElse doesn't make sense. Look at the resulting SQL...
        return new SistOppdatert(oppdatert);

    }

    @GetMapping("antallUleste")
    public AntallUlesteDTO antallUleste() {

        long antall = dialogDataService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(auth.erEksternBruker() ? DialogData::erUlestForBruker : DialogData::erUlestAvVeileder)
                .filter(it -> !it.isHistorisk())
                .count();
        return new AntallUlesteDTO(toIntExact(antall));

    }

    @GetMapping("{dialogId}")
    public DialogDTO hentDialog(@PathVariable String dialogId) {
        return Optional.ofNullable(dialogId)
                .map(Long::parseLong)
                .map(dialogDataService::hentDialog)
                .map(restMapper::somDialogDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // TODO: 12/03/2021 flytt logikk inn i servicen denne burde ikke trenge og vere transactional
    @PostMapping
    @Transactional
    public DialogDTO nyHenvendelse(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        slettUtdattertKladd(nyHenvendelseDTO);

        long dialogId = finnDialogId(nyHenvendelseDTO);
        dialogDataService.opprettHenvendelseForDialog(HenvendelseData.builder()
                .dialogId(dialogId)
                .avsenderId(auth.getIdent().orElse(null))
                .viktig(!nyHenvendelseDTO.egenskaper.isEmpty())
                .avsenderType(auth.erEksternBruker() ? AvsenderType.BRUKER : AvsenderType.VEILEDER)
                .tekst(nyHenvendelseDTO.tekst)
                .build()
        );
        DialogData dialogData = markerSomLest(dialogId);
        dialogDataService.updateDialogAktorFor(dialogData.getAktorId());

        return kontorsperreFilter.filterKontorsperre(dialogData) ?
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
        return kontorsperreFilter.filterKontorsperre(dialogData) ?
                restMapper.somDialogDTO(dialogData)
                : null;
    }

    private DialogData markerSomLest(long dialogId) {
        if (auth.erEksternBruker()) {
            return dialogDataService.markerDialogSomLestAvBruker(dialogId);
        }
        return dialogDataService.markerDialogSomLestAvVeileder(dialogId);

    }

    @PutMapping("{dialogId}/venter_pa_svar/{venter}")
    public DialogDTO oppdaterVenterPaSvar(@PathVariable String dialogId, @PathVariable boolean venter) {
        auth.skalVereInternBruker();

        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .venterPaSvar(venter)
                .build();

        DialogData dialog = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);
        dialogDataService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @PutMapping("{dialogId}/ferdigbehandlet/{ferdigbehandlet}")
    public DialogDTO oppdaterFerdigbehandlet(@PathVariable String dialogId, @PathVariable boolean ferdigbehandlet) {
        auth.skalVereInternBruker();

        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .ferdigbehandlet(ferdigbehandlet)
                .build();

        DialogData dialog = dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogStatus);
        dialogDataService.updateDialogAktorFor(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @PostMapping("forhandsorientering")
    public DialogDTO forhandsorienteringPaAktivitet(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        auth.skalVereInternBruker();

        long dialogId = finnDialogId(nyHenvendelseDTO);
        dialogDataService.updateDialogEgenskap(EgenskapType.PARAGRAF8, dialogId);
        dialogDataService.markerSomParagra8(dialogId);
        return nyHenvendelse(nyHenvendelseDTO.setEgenskaper(singletonList(Egenskap.PARAGRAF8)));
    }

    private Long finnDialogId(NyHenvendelseDTO nyHenvendelseDTO) {
        if (!StringUtils.isEmpty(nyHenvendelseDTO.dialogId)) {
            return Long.parseLong(nyHenvendelseDTO.dialogId);
        } else {
            return Optional.ofNullable(nyHenvendelseDTO.aktivitetId)
                    .filter(StringUtils::isNotEmpty)
                    .flatMap(dialogDataService::hentDialogForAktivitetId)
                    .orElseGet(() -> opprettDialog(nyHenvendelseDTO))
                    .getId();
        }
    }

    private DialogData opprettDialog(NyHenvendelseDTO nyHenvendelseDTO) {
        DialogData dialogData = DialogData.builder()
                .overskrift(nyHenvendelseDTO.overskrift)
                .aktorId(dialogDataService.hentAktoerIdForPerson(getContextUserIdent()))
                .aktivitetId(nyHenvendelseDTO.aktivitetId)
                .egenskaper(nyHenvendelseDTO.egenskaper
                        .stream()
                        .map(egenskap -> EgenskapType.valueOf(egenskap.name()))
                        .collect(Collectors.toList()))
                .build();
        DialogData opprettetDialog = dialogDataService.opprettDialogForAktivitetsplanPaIdent(dialogData);

        if (auth.erEksternBruker()) {
            funksjonelleMetrikker.nyDialogBruker(opprettetDialog);
        } else if (auth.erInternBruker()) {
            funksjonelleMetrikker.nyDialogVeileder(opprettetDialog);
        }


        return opprettetDialog;
    }

    private Person getContextUserIdent() {
        if (auth.erEksternBruker()) {
            return auth.getIdent().map(Person::fnr).orElseThrow(RuntimeException::new);
        }
        Optional<Person> fnr = Optional
                .ofNullable(httpServletRequest.getParameter("fnr"))
                .map(Person::fnr);
        Optional<Person> aktorId = Optional
                .ofNullable(httpServletRequest.getParameter("aktorId"))
                .map(Person::aktorId);
        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new)); // TODO: Fix error handling (here: missing parameters).
    }
}
