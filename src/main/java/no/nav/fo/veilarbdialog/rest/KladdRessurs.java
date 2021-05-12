package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.Kladd;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.service.KladdService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(
        value = "/api/kladd",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class KladdRessurs {

    private final KladdService kladdService;
    private final HttpServletRequest httpServletRequest;
    private final AuthService auth;

    @GetMapping
    public List<KladdDTO> hentKladder() {
        return kladdService.hentKladder(getContextUserIdent())
                .stream()
                .map(KladdRessurs::somKladdDTO)
                .collect(toList());
    }

    @PostMapping
    public void oppdaterKladd(@RequestBody KladdDTO dto) {
        kladdService.upsertKladd(getContextUserIdent(), somKladd(dto));
    }

    private String getContextUserIdent() {
        if (auth.erEksternBruker()) {
            return auth.getIdent().orElseThrow(RuntimeException::new);
        }
        return Optional
                .ofNullable(httpServletRequest.getParameter("fnr"))
                .orElseThrow(RuntimeException::new);
    }

    private static KladdDTO somKladdDTO(Kladd kladd) {
        return KladdDTO.builder()
                .aktivitetId(kladd.getAktivitetId())
                .dialogId(kladd.getDialogId())
                .overskrift(kladd.getOverskrift())
                .tekst(kladd.getTekst())
                .build();
    }

    private static Kladd somKladd(KladdDTO dto) {
        return Kladd.builder()
                .aktivitetId(dto.getAktivitetId())
                .dialogId(dto.getDialogId())
                .overskrift(dto.getOverskrift())
                .tekst(dto.getTekst())
                .build();
    }

}
