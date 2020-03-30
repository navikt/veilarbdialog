package no.nav.fo.veilarbdialog.rest;


import no.nav.common.auth.SubjectHandler;
import no.nav.fo.veilarbdialog.domain.Kladd;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.service.KladdService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbdialog.service.AutorisasjonService.erEksternBruker;

@Component
@Path("/kladd")
public class KladdRessurs {

    @Inject
    private KladdService kladdService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @GET
    public List<KladdDTO> hentKladder() {
        return kladdService.hentKladder(getContextUserIdent())
                .stream()
                .map(KladdRessurs::somKladdDTO)
                .collect(toList());
    }

    @POST
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
