package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.GjeldendeEskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerIkkeUnderOppfolgingException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping(
        value = "/api/eskaleringsvarsel",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@Slf4j
public class EskaleringsvarselController {

    private final EskaleringsvarselService eskaleringsvarselService;

    private final IAuthService authService;

    private void skalVereInternBruker() {
        if (!authService.erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig bruker type");
        }
    }

    @PostMapping(value = "/start")
    public EskaleringsvarselDto start(@RequestBody StartEskaleringDto startEskaleringDto) {
        skalVereInternBruker();
        authService.sjekkTilgangTilPerson(startEskaleringDto.fnr());

        EskaleringsvarselEntity eskaleringsvarselEntity = eskaleringsvarselService.start(startEskaleringDto.fnr(), startEskaleringDto.begrunnelse(), startEskaleringDto.overskrift(), startEskaleringDto.tekst());

        return eskaleringsvarselEntity2Dto(eskaleringsvarselEntity);
    }

    @PatchMapping("/stop")
    public void stop(@RequestBody StopEskaleringDto stopEskaleringDto) {
        skalVereInternBruker();
        authService.sjekkTilgangTilPerson(stopEskaleringDto.fnr());
        NavIdent navIdent = authService.getInnloggetVeilederIdent();

        Optional<EskaleringsvarselEntity> eskaleringsvarselEntity = eskaleringsvarselService.stop(stopEskaleringDto.fnr(), stopEskaleringDto.begrunnelse(), stopEskaleringDto.skalSendeHenvendelse(), navIdent);
        if (eskaleringsvarselEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ingen gjeldende eskaleringsvarsel");
        }
    }

    @GetMapping(value = "/gjeldende")
    public ResponseEntity<GjeldendeEskaleringsvarselDto> hentGjeldende(@RequestParam(required = false) Fnr fnr) {
        Fnr fodselsnummer;
        if (fnr == null) { // eksternbruker
            if (authService.erEksternBruker()) {
                fodselsnummer = (Fnr) authService.getLoggedInnUser();
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Internbruker må sende med fnr som parameter");
            }
        } else { // internbruker
            fodselsnummer = fnr;
        }

        authService.sjekkTilgangTilPerson(fodselsnummer);

        Optional<EskaleringsvarselEntity> maybeGjeldende = eskaleringsvarselService.hentGjeldende(fodselsnummer);

        return maybeGjeldende
                .map(g -> ResponseEntity.ok(gjeldendeEskaleringsvarselDto(g)))
                .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @GetMapping(value = "/historikk", params = "fnr")
    public List<EskaleringsvarselDto> historikk(@RequestParam Fnr fnr) {
        skalVereInternBruker();
        authService.sjekkTilgangTilPerson(fnr);

        return eskaleringsvarselService.historikk(fnr)
                .stream()
                .map(EskaleringsvarselController::eskaleringsvarselEntity2Dto)
                .toList();
    }

    @ExceptionHandler({BrukerKanIkkeVarslesException.class, BrukerIkkeUnderOppfolgingException.class, AktivEskaleringException.class})
    public ResponseEntity<String> handleExceptions(Exception e) {
        String feilmelding = String.format("Funksjonell feil under behandling: %s - %s ", e.getClass().getSimpleName(), e.getMessage());
        log.warn(feilmelding);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(feilmelding);

    }


    public static EskaleringsvarselDto eskaleringsvarselEntity2Dto(EskaleringsvarselEntity entity) {
        return new EskaleringsvarselDto(entity.varselId(), entity.tilhorendeDialogId(), entity.opprettetAv(), entity.opprettetDato(), entity.opprettetBegrunnelse(), entity.avsluttetDato(), entity.avsluttetAv(), entity.avsluttetBegrunnelse());
    }

    public static GjeldendeEskaleringsvarselDto gjeldendeEskaleringsvarselDto(EskaleringsvarselEntity entity) {
        return new GjeldendeEskaleringsvarselDto(entity.varselId(), entity.tilhorendeDialogId(), entity.opprettetAv(), entity.opprettetDato(), entity.opprettetBegrunnelse());
    }

}
