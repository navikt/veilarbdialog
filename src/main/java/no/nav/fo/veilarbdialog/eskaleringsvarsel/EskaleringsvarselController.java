package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping(
        value = "/api/eskaleringsvarsel",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class EskaleringsvarselController {

    private final EskaleringsvarselService eskaleringsvarselService;

    @PostMapping(value = "/start")
    /**
     * Returnerer henvendelsesId til tilhørende dialog
     */
    public EskaleringsvarselDto start(@RequestBody StartEskaleringDto startEskaleringDto) {
        EskaleringsvarselEntity eskaleringsvarselEntity = eskaleringsvarselService.start(startEskaleringDto.fnr(), startEskaleringDto.begrunnelse(), startEskaleringDto.overskrift(), startEskaleringDto.tekst());

        return eskaleringsvarselEntity2Dto(eskaleringsvarselEntity);
    }

    @PatchMapping("/stop")
    public void stop(@RequestBody StopEskaleringDto stopEskaleringDto) {

    }

    @GetMapping(value = "/gjeldende", params = "aktorId")
    public EskaleringsvarselDto hentGjeldende(@RequestParam AktorId aktorId) {
        return null;
    }

    @GetMapping("/historikk")
    public List<EskaleringsvarselDto> historikk() {
        return null;
    }


    public static EskaleringsvarselDto eskaleringsvarselEntity2Dto(EskaleringsvarselEntity entity) {
        return new EskaleringsvarselDto(entity.varselId(), entity.tilhorendeDialogId(), entity.opprettetAv(), entity.opprettetDato(), entity.opprettetBegrunnelse(), entity.avsluttetDato(), entity.avsluttetAv(), entity.avsluttetBegrunnelse());
    }

}
