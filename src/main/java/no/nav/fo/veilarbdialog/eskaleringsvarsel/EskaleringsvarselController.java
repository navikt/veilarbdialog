package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping(
        value = "/api/eskaleringsvarsel",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class EskaleringsvarselController {

    private final EskaleringsvarselService eskaleringsvarselService;

    private final AuthService authService;

    @PostMapping(value = "/start")
    /**
     * Returnerer henvendelsesId til tilhørende dialog
     */
    public EskaleringsvarselDto start(@RequestBody StartEskaleringDto startEskaleringDto) {
        authService.skalVereInternBruker();
        authService.harTilgangTilPerson(startEskaleringDto.fnr());

        EskaleringsvarselEntity eskaleringsvarselEntity = eskaleringsvarselService.start(startEskaleringDto.fnr(), startEskaleringDto.begrunnelse(), startEskaleringDto.overskrift(), startEskaleringDto.tekst());

        return eskaleringsvarselEntity2Dto(eskaleringsvarselEntity);
    }

    @PatchMapping("/stop")
    public void stop(@RequestBody StopEskaleringDto stopEskaleringDto) {
        authService.skalVereInternBruker();
        authService.harTilgangTilPerson(stopEskaleringDto.fnr());
        NavIdent navIdent = authService.getNavIdent();

        eskaleringsvarselService.stop(stopEskaleringDto.fnr(), stopEskaleringDto.begrunnelse(), navIdent);
    }

    @GetMapping(value = "/gjeldende", params = "fnr")
    public ResponseEntity<EskaleringsvarselDto> hentGjeldende(@RequestParam Fnr fnr) {
        authService.skalVereInternBruker();
        authService.harTilgangTilPerson(fnr);

        Optional<EskaleringsvarselEntity> maybeGjeldende = eskaleringsvarselService.hentGjeldende(fnr);

        return maybeGjeldende
                .map((g) -> ResponseEntity.ok(eskaleringsvarselEntity2Dto(g)))
                .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @GetMapping(value = "/historikk", params = "fnr")
    public List<EskaleringsvarselDto> historikk(@RequestParam Fnr fnr) {
        authService.skalVereInternBruker();
        authService.harTilgangTilPerson(fnr);

        return eskaleringsvarselService.historikk(fnr)
                .stream()
                .map(EskaleringsvarselController::eskaleringsvarselEntity2Dto)
                .collect(Collectors.toList());
    }


    public static EskaleringsvarselDto eskaleringsvarselEntity2Dto(EskaleringsvarselEntity entity) {
        return new EskaleringsvarselDto(entity.varselId(), entity.tilhorendeDialogId(), entity.opprettetAv(), entity.opprettetDato(), entity.opprettetBegrunnelse(), entity.avsluttetDato(), entity.avsluttetAv(), entity.avsluttetBegrunnelse());
    }

}
