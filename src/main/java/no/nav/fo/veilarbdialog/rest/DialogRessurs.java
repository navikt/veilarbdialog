package no.nav.fo.veilarbdialog.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.fo.veilarbdialog.service.PersonService;
import no.nav.fo.veilarbdialog.util.DialogResource;
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_a2_annotations.auth.OnlyInternBruker;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.TilgangsType;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static java.lang.Math.toIntExact;
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
    private final KvpService kvpService;
    private final IAuthService auth;
    private final PersonService personService;

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

    @PostMapping("antallUleste")
    public AntallUlesteDTO antallUlestePost(@RequestBody(required = false) FnrDto fnrDto) {
        var fnr = fnrDto != null && fnrDto.fnr != null ? Person.fnr(fnrDto.fnr) : getContextUserIdent();
        auth.sjekkTilgangTilPerson(fnr.eksternBrukerId(), TilgangsType.LESE);
        return innterAntallUleste(fnr);
    }

    @GetMapping("antallUleste")
    @AuthorizeFnr(auditlogMessage = "hent antall uleste")
    public AntallUlesteDTO antallUleste() {
        var fnr = getContextUserIdent();
        return innterAntallUleste(fnr);
    }

    private AntallUlesteDTO innterAntallUleste(Person fnr) {
        long antall = dialogDataService.hentDialogerForBruker(fnr)
                .stream()
                .filter(auth.erEksternBruker() ? DialogData::erUlestForBruker : DialogData::erUlestAvVeileder)
                .filter(it -> !it.isHistorisk())
                .count();
        return new AntallUlesteDTO(toIntExact(antall));
    }

    @PostMapping("sistOppdatert")
    public SistOppdatertDTO sistOppdatertPost(@RequestBody(required = false) FnrDto fnrDto) {
        var fnr = fnrDto != null && fnrDto.fnr != null ? Person.fnr(fnrDto.fnr) : getContextUserIdent();
        auth.sjekkTilgangTilPerson(fnr.eksternBrukerId(), TilgangsType.LESE);
        return internSistOppdatert(fnr);
    }

    @GetMapping("sistOppdatert")
    @AuthorizeFnr()
    public SistOppdatertDTO sistOppdatert() {
        return internSistOppdatert(getContextUserIdent());
    }

    private SistOppdatertDTO internSistOppdatert(Person fnr) {
        var oppdatert = dialogDataService.hentSistOppdatertForBruker(fnr, auth.getLoggedInnUser());
        return new SistOppdatertDTO(oppdatert == null ? null : oppdatert.getTime());
    }

    @GetMapping("{dialogId}")
    @AuthorizeFnr(auditlogMessage = "hent dialog", resourceIdParamName = "dialogId", resourceType = DialogResource.class)
    public DialogDTO hentDialog(@PathVariable Long dialogId) {
        DialogData dialogData = dialogDataService.hentDialog(dialogId);
        return restMapper.somDialogDTO(dialogData);
    }

    private void sjekkTilgangOgAuditlog(EksternBrukerId bruker) {
        var subject = auth.getLoggedInnUser();
        try {
            if (auth.erEksternBruker()) {
                auth.sjekkTilgangTilPerson(bruker, TilgangsType.SKRIVE);
            } else {
                auth.sjekkInternbrukerHarSkriveTilgangTilPerson(bruker);
            }
        } catch (Exception e) {
            auth.auditlog(false, subject , bruker, "ny melding");
            throw e;
        }
        // Litt tidlig audit-logging men men
        auth.auditlog(true, subject , bruker, "ny melding");
    }

    @PostMapping
    public DialogDTO nyMelding(@RequestBody NyMeldingDTO nyMeldingDTO) {
        Person bruker = nyMeldingDTO.getFnr() != null ? Person.fnr(nyMeldingDTO.getFnr()) : getContextUserIdent();
        sjekkTilgangOgAuditlog(bruker.eksternBrukerId());

        var dialogData = dialogDataService.opprettMelding(nyMeldingDTO, bruker, !auth.erEksternBruker());
        if (nyMeldingDTO.getVenterPaaSvarFraNav() != null) {
            dialogData = dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogData.getId(), !nyMeldingDTO.getVenterPaaSvarFraNav());
            dialogDataService.sendPaaKafka(dialogData.getAktorId());
        }
        if (nyMeldingDTO.getVenterPaaSvarFraBruker() != null) {
            var dialogStatus = DialogStatus.builder()
                    .dialogId(dialogData.getId())
                    .venterPaSvar(nyMeldingDTO.getVenterPaaSvarFraBruker())
                    .build();

            dialogData = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);
            dialogDataService.sendPaaKafka(dialogData.getAktorId());
        }
        // Vi er ikke helt sikre på hvotfor dette er sånn
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
}
