package no.nav.fo.veilarbdialog.rest;


import lombok.RequiredArgsConstructor;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.fo.veilarbdialog.domain.Kladd;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.service.KladdService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.service.AutorisasjonService.erEksternBruker;

@RestController
@RequestMapping("/kladd")
@RequiredArgsConstructor
public class KladdRessurs {

    private final KladdService kladdService;
    private final ServiceLoader.Provider<HttpServletRequest> requestProvider;

    @GetMapping
    public List<KladdDTO> hentKladder() {
        return kladdService.hentKladder(getContextUserIdent())
                .stream()
                .map(KladdRessurs::somKladdDTO)
                .collect(toList());
    }

    @PostMapping
    public void oppdaterKladd(KladdDTO dto) {
        kladdService.upsertKladd(getContextUserIdent(), somKladd(dto));
    }

    private String getContextUserIdent() {
        if (erEksternBruker()) {
            return SubjectHandler.getIdent().orElseThrow(RuntimeException::new);
        }
        return Optional.ofNullable(requestProvider.get().getParameter("fnr")).orElseThrow(RuntimeException::new);
    }

    private static KladdDTO somKladdDTO(Kladd kladd) {
        return KladdDTO.builder()
                .aktivitetId(kladd.aktivitetId)
                .dialogId(kladd.dialogId)
                .overskrift(kladd.overskrift)
                .tekst(kladd.tekst)
                .build();
    }

    private static Kladd somKladd(KladdDTO dto) {
        return Kladd.builder()
                .aktivitetId(dto.aktivitetId)
                .dialogId(dto.dialogId)
                .overskrift(dto.overskrift)
                .tekst(dto.tekst)
                .build();
    }

}
