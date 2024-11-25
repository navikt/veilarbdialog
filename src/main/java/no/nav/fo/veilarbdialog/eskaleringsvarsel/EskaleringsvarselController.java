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
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_a2_annotations.auth.OnlyInternBruker;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.TilgangsType;
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

    @PostMapping(value = "/start")
    @OnlyInternBruker
    public EskaleringsvarselDto start(@RequestBody StartEskaleringDto startEskaleringDto) {
        authService.sjekkTilgangTilPerson(startEskaleringDto.fnr(), TilgangsType.LESE);
        EskaleringsvarselEntity eskaleringsvarselEntity = eskaleringsvarselService.start(startEskaleringDto);
        return EskaleringsvarselDto.fromEntity(eskaleringsvarselEntity);
    }

    @PatchMapping("/stop")
    @OnlyInternBruker
    public void stop(@RequestBody StopEskaleringDto stopEskaleringDto) {
        authService.sjekkTilgangTilPerson(stopEskaleringDto.fnr(), TilgangsType.LESE);
        NavIdent navIdent = authService.getInnloggetVeilederIdent();

        Optional<EskaleringsvarselEntity> eskaleringsvarselEntity = eskaleringsvarselService.stop(stopEskaleringDto, navIdent);
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

        authService.sjekkTilgangTilPerson(fodselsnummer, TilgangsType.LESE);

        Optional<EskaleringsvarselEntity> maybeGjeldende = eskaleringsvarselService.hentGjeldende(fodselsnummer);

        return maybeGjeldende
                .map(g -> ResponseEntity.ok(gjeldendeEskaleringsvarselDto(g)))
                .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @GetMapping(value = "/historikk", params = "fnr")
    @OnlyInternBruker
    @AuthorizeFnr(auditlogMessage = "eskaleringsvarselhistorikk")
    public List<EskaleringsvarselDto> historikk(@RequestParam Fnr fnr) {
        return eskaleringsvarselService.historikk(fnr)
                .stream()
                .map(EskaleringsvarselDto::fromEntity)
                .toList();
    }

    public static GjeldendeEskaleringsvarselDto gjeldendeEskaleringsvarselDto(EskaleringsvarselEntity entity) {
        return new GjeldendeEskaleringsvarselDto(entity.varselId(), entity.tilhorendeDialogId(), entity.opprettetAv(), entity.opprettetDato(), entity.opprettetBegrunnelse());
    }

}
