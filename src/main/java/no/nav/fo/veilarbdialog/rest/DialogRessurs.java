package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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

    @GetMapping
    public List<DialogDTO> hentDialoger() {
        return dialogDataService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(kontorsperreFilter::tilgangTilEnhet)
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
                .map(dialogDataService::hentDialogMedTilgangskontroll)
                .map(restMapper::somDialogDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public DialogDTO nyHenvendelse(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        Person bruker = getContextUserIdent();
        DialogData dialogData = dialogDataService.opprettHenvendelse(nyHenvendelseDTO, bruker);

        return kontorsperreFilter.tilgangTilEnhet(dialogData) ?
                restMapper.somDialogDTO(dialogData)
                : null;

    }

    @PutMapping("{dialogId}/les")
    public DialogDTO markerSomLest(@PathVariable String dialogId) {
        DialogData dialogData = dialogDataService.markerDialogSomLest(Long.parseLong(dialogId));
        return kontorsperreFilter.tilgangTilEnhet(dialogData) ?
                restMapper.somDialogDTO(dialogData)
                : null;
    }

    @PutMapping("{dialogId}/venter_pa_svar/{venter}")
    public DialogDTO oppdaterVenterPaSvar(@PathVariable String dialogId, @PathVariable boolean venter) {
        auth.skalVereInternBruker();

        DialogStatus dialogStatus = DialogStatus.builder()
                .dialogId(Long.parseLong(dialogId))
                .venterPaSvar(venter)
                .build();

        DialogData dialog = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);
        dialogDataService.sendPaaKafka(dialog.getAktorId());

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
        dialogDataService.sendPaaKafka(dialog.getAktorId());

        return markerSomLest(dialogId);
    }

    @PostMapping("forhandsorientering")
    public DialogDTO forhandsorienteringPaAktivitet(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        String aktorId = dialogDataService.hentAktoerIdForPerson(getContextUserIdent());
        auth.harTilgangTilPersonEllerKastIngenTilgang(aktorId);

        auth.skalVereInternBruker();

        DialogData dialog = dialogDataService.hentDialogMedTilgangskontroll(nyHenvendelseDTO.dialogId, nyHenvendelseDTO.aktivitetId);
        if (dialog == null) dialog = dialogDataService.opprettDialog(nyHenvendelseDTO, aktorId);

        long dialogId = dialog.getId();
        dialogDataService.updateDialogEgenskap(EgenskapType.PARAGRAF8, dialogId);
        dialogDataService.markerSomParagra8(dialogId);
        return nyHenvendelse(nyHenvendelseDTO.setEgenskaper(singletonList(Egenskap.PARAGRAF8)));
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
