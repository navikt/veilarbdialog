package no.nav.fo.veilarbdialog.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.domain.Kladd;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.service.KladdService;
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(
        value = "/api/kladd",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class KladdRessurs {

    private final KladdService kladdService;
    private final HttpServletRequest httpServletRequest;
    private final IAuthService auth;
    private final IAuthService authService;

    @GetMapping
    @AuthorizeFnr()
    public List<KladdDTO> hentKladder() {
        return kladdService.hentKladder(getContextUserIdent(null))
                .stream()
                .map(KladdRessurs::somKladdDTO)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void oppdaterKladd(@RequestBody KladdDTO kladd) {
        var fnr = getContextUserIdent(kladd);
        authService.sjekkTilgangTilPerson(Fnr.of(fnr));
        kladdService.upsertKladd(fnr, somKladd(kladd));
    }

    private String getContextUserIdent(KladdDTO kladd) {
        if (auth.erEksternBruker()) {
            return auth.getLoggedInnUser().get();
        }
        return Optional
                .ofNullable(httpServletRequest.getParameter("fnr"))
                .or(() -> fnrFromRequest(httpServletRequest))
                .or(() -> kladd != null ? Optional.ofNullable(kladd.getFnr()) : Optional.empty() )
                .orElseThrow(RuntimeException::new);
    }

    private Optional<String> fnrFromRequest(HttpServletRequest request) {
        try {
            var fnr = ((Fnr) request.getAttribute("fnr")).get();
            return Optional.of(fnr);
        } catch (Exception e) {
            return Optional.empty();
        }
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
