package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.EskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/eskaleringsvarsel")
public class EskaleringsvarselController {

    private final EskaleringsvarselService eskaleringsvarselService;

    @PostMapping("/start")
    public void start(@RequestBody StartEskaleringDto startEskaleringDto) {

    }

    @PatchMapping("/stop")
    public void stop(@RequestBody StopEskaleringDto stopEskaleringDto) {

    }

    @GetMapping("/gjeldende")
    public EskaleringsvarselDto hentGjeldende() {
        return null;
    }

    @GetMapping("/historikk")
    public List<EskaleringsvarselDto> historikk() {
        return null;
    }

}
