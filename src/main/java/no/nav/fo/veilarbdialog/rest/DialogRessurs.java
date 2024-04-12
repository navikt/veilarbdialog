package no.nav.fo.veilarbdialog.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.fo.veilarbdialog.util.DialogResource;
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_a2_annotations.auth.OnlyInternBruker;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
    private final IAuthService auth;

    @GetMapping
    @AuthorizeFnr(auditlogMessage = "hent dialoger")
    public List<DialogDTO> hentDialoger(@RequestParam(value = "ekskluderDialogerMedKontorsperre", required = false) boolean ekskluderDialogerMedKontorsperre) {
        return dialogDataService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter((dialog) ->
                     ekskluderDialogerMedKontorsperre ?
                            dialog.getKontorsperreEnhetId() == null || dialog.getKontorsperreEnhetId().isBlank()
                    : true
                )
                .filter(kontorsperreFilter::tilgangTilEnhet)
                .map(restMapper::somDialogDTO)
                .collect(toList());
    }

    @GetMapping("antallUleste")
    @AuthorizeFnr(auditlogMessage = "hent antall uleste")
    public AntallUlesteDTO antallUleste() {

        long antall = dialogDataService.hentDialogerForBruker(getContextUserIdent())
                .stream()
                .filter(auth.erEksternBruker() ? DialogData::erUlestForBruker : DialogData::erUlestAvVeileder)
                .filter(it -> !it.isHistorisk())
                .count();
        return new AntallUlesteDTO(toIntExact(antall));

    }

    @GetMapping("sistOppdatert")
    @AuthorizeFnr()
    public SistOppdatertDTO sistOppdatert() {
        var oppdatert = dialogDataService.hentSistOppdatertForBruker(getContextUserIdent(), auth.getLoggedInnUser());
        return new SistOppdatertDTO(oppdatert == null ? null : oppdatert.getTime());
    }

    @GetMapping("{dialogId}")
    @AuthorizeFnr(auditlogMessage = "hent dialog", resourceIdParamName = "dialogId", resourceType = DialogResource.class)
    public DialogDTO hentDialog(@PathVariable Long dialogId) {
        DialogData dialogData = dialogDataService.hentDialog(dialogId);
        return restMapper.somDialogDTO(dialogData);
    }

    @PostMapping
    @AuthorizeFnr(auditlogMessage = "ny henvendelse")
    public DialogDTO nyHenvendelse(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        Person bruker = getContextUserIdent();
        var dialogData = dialogDataService.opprettHenvendelse(nyHenvendelseDTO, bruker);
        if (nyHenvendelseDTO.getVenterPaaSvarFraNav() != null) {
            dialogData = dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogData.getId(), !nyHenvendelseDTO.getVenterPaaSvarFraNav());
            dialogDataService.sendPaaKafka(dialogData.getAktorId());
        }
        if (nyHenvendelseDTO.getVenterPaaSvarFraBruker() != null) {
            var dialogStatus = DialogStatus.builder()
                    .dialogId(dialogData.getId())
                    .venterPaSvar(nyHenvendelseDTO.getVenterPaaSvarFraBruker())
                    .build();

            dialogData = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);
            dialogDataService.sendPaaKafka(dialogData.getAktorId());
        }
        return kontorsperreFilter.tilgangTilEnhet(dialogData) ?
                restMapper.somDialogDTO(dialogData)
                : null;

    }

    @PutMapping("{dialogId}/les")
    @AuthorizeFnr(auditlogMessage = "les dialog", resourceIdParamName = "dialogId", resourceType = DialogResource.class)
    @Transactional
    public DialogDTO markerSomLest(@PathVariable Long dialogId) {
        var dialogData = dialogDataService.markerDialogSomLest(dialogId);
        return restMapper.somDialogDTO(dialogData);
    }

    @PutMapping("{dialogId}/venter_pa_svar/{venter}")
    @AuthorizeFnr(auditlogMessage = "hent dialog", resourceIdParamName = "dialogId", resourceType = DialogResource.class)
    @OnlyInternBruker
    @Transactional
    public DialogDTO oppdaterVenterPaSvar(@PathVariable Long dialogId, @PathVariable boolean venter) {
        var dialogStatus = DialogStatus.builder()
                .dialogId(dialogId)
                .venterPaSvar(venter)
                .build();
        var dialog = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);
        dialogDataService.sendPaaKafka(dialog.getAktorId());
        return markerSomLest(dialogId);
    }

    @PutMapping("{dialogId}/ferdigbehandlet/{ferdigbehandlet}")
    @AuthorizeFnr(auditlogMessage = "hent dialog", resourceIdParamName = "dialogId", resourceType = DialogResource.class)
    @OnlyInternBruker
    public DialogDTO oppdaterFerdigbehandlet(@PathVariable Long dialogId, @PathVariable boolean ferdigbehandlet) {
        var dialog = dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogId, ferdigbehandlet);
        dialogDataService.sendPaaKafka(dialog.getAktorId());
        return markerSomLest(dialogId);
    }

    @PostMapping("forhandsorientering")
    @AuthorizeFnr(auditlogMessage = "forhåndsorientering på aktivitet")
    @OnlyInternBruker
    public DialogDTO forhandsorienteringPaAktivitet(@RequestBody NyHenvendelseDTO nyHenvendelseDTO) {
        var aktorId = dialogDataService.hentAktoerIdForPerson(getContextUserIdent());

        var dialog = dialogDataService.hentDialog(nyHenvendelseDTO.getDialogId(),
               AktivitetId.of(nyHenvendelseDTO.getAktivitetId()));
        if (dialog == null) dialog = dialogDataService.opprettDialog(nyHenvendelseDTO, aktorId.get());

        long dialogId = dialog.getId();
        dialogDataService.updateDialogEgenskap(EgenskapType.PARAGRAF8, dialogId);
        dialogDataService.markerSomParagra8(dialogId);
        return nyHenvendelse(nyHenvendelseDTO.setEgenskaper(singletonList(Egenskap.PARAGRAF8)));
    }


    private Person getContextUserIdent() {
        if (auth.erEksternBruker()) {
            var user = auth.getLoggedInnUser();
            return Person.fnr(user.get());
        }
        Optional<Person> fnr = Optional
                .ofNullable(httpServletRequest.getParameter("fnr"))
                .map(Person::fnr);
        Optional<Person> aktorId = Optional
                .ofNullable(httpServletRequest.getParameter("aktorId"))
                .map(Person::aktorId);
        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new));
    }

    private void sjekkTilgangTilDialog(DialogData dialogData) {
        auth.sjekkTilgangTilPerson(AktorId.of(dialogData.getAktorId()));
        dialogData.getKontorEnhet().ifPresent(auth::sjekkTilgangTilEnhet);
    }
}
