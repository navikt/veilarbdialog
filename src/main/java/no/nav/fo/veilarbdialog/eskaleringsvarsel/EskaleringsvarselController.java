package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/eskaleringsvarsel", produces = MediaType.APPLICATION_JSON_VALUE)
public class EskaleringsvarselController {

    private final EskaleringsvarselService eskaleringsvarselService;

    @PostMapping(value = "/start")
    /**
     * Returnerer henvendelsesId til tilhørende dialog
     */
    public EskaleringsvarselDto start(@RequestBody StartEskaleringDto startEskaleringDto) {
        // wait
        return null;
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

}
