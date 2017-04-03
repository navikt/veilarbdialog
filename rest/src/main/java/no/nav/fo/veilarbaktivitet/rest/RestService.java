package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.fo.veilarbdialog.api.AktivitetController;
import no.nav.fo.veilarbdialog.domain.AktivitetDTO;
import no.nav.fo.veilarbdialog.domain.AktivitetStatus;
import no.nav.fo.veilarbdialog.domain.AktivitetsplanDTO;
import no.nav.fo.veilarbdialog.domain.EndringsloggDTO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.util.EnumUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class RestService implements AktivitetController {

    @Inject
    private AppService appService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Override
    public AktivitetsplanDTO hentAktivitetsplan() {
        val aktiviter = appService.hentAktiviteterForIdent(getUserIdent())
                .stream()
                .map(RestMapper::mapTilAktivitetDTO)
                .collect(Collectors.toList());

        return new AktivitetsplanDTO().setAktiviteter(aktiviter);
    }

    @Override
    public AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(RestMapper::mapTilAktivitetData)
                .map((aktivitetData) -> appService.opprettNyAktivtet(getUserIdent(), aktivitetData))
                .map(RestMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public AktivitetDTO oppdaterAktiviet(AktivitetDTO aktivitet) {
        throw new RuntimeException();
    }

    @Override
    public void slettAktivitet(String aktivitetId) {
        appService.slettAktivitet(Long.parseLong(aktivitetId));
    }

    @Override
    public AktivitetDTO oppdaterStatus(String aktivitetId, String status) {
        val aktivitet = appService.oppdaterStatus(Long.parseLong(aktivitetId),
                EnumUtils.valueOf(AktivitetStatus.class, status));

        return RestMapper.mapTilAktivitetDTO(aktivitet);
    }

    @Override
    public List<EndringsloggDTO> hentEndringsLoggForAktivitetId(String aktivitetId) {
        return Optional.of(aktivitetId)
                .map(Long::parseLong)
                .map(aId -> appService.hentEndringsloggForAktivitetId(aId))
                .map((endringslist) -> endringslist.stream()
                        .map(RestMapper::mapEndringsLoggDTO)
                        .collect(Collectors.toList())
                ).orElseThrow(RuntimeException::new);
    }

    private String getUserIdent() {
        return Optional.ofNullable(requestProvider.get().getParameter("fnr"))
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }
}
