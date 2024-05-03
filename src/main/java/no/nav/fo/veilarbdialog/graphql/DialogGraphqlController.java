package no.nav.fo.veilarbdialog.graphql;

import lombok.AllArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.GjeldendeEskaleringsvarselDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.rest.KladdRessurs;
import no.nav.fo.veilarbdialog.rest.RestMapper;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.fo.veilarbdialog.service.KladdService;
import no.nav.poao.dab.spring_auth.AuthService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.Math.toIntExact;

@AllArgsConstructor
@Controller
public class DialogGraphqlController {

    final private AuthService authService;
    final private DialogDataService dialogDataService;
    final private RestMapper restMapper;
    final private KontorsperreFilter kontorsperreFilter;
    final private EskaleringsvarselService eskaleringsvarselService;
    final private KladdService kladdService;


    @QueryMapping
    public List<DialogDTO> dialoger(@Argument String fnr, @Argument Optional<Boolean> bareMedAktiviteter) {
        var targetFnr = Fnr.of(getContextUserIdent(fnr).get());
        authService.sjekkTilgangTilPerson(targetFnr);
        return dialogDataService.hentDialogerForBruker(Person.fnr(targetFnr.get()))
                .stream()
                .filter(bareMedAktiviteterFilter(bareMedAktiviteter))
                .filter(kontorsperreFilter::tilgangTilEnhet)
                .map(restMapper::somDialogDTO).toList();
    }

    @QueryMapping
    public GjeldendeEskaleringsvarselDto stansVarsel(@Argument String fnr) {
        var targetFnr = Fnr.of(getContextUserIdent(fnr).get());
        authService.sjekkTilgangTilPerson(targetFnr);
        return eskaleringsvarselService.hentGjeldende(targetFnr)
                .map(varsel -> new GjeldendeEskaleringsvarselDto(
                        varsel.varselId(),
                        varsel.tilhorendeDialogId(),
                        varsel.opprettetAv(),
                        varsel.opprettetDato(),
                        varsel.opprettetBegrunnelse()
                )).orElse(null);
    }

    @QueryMapping
    public List<KladdDTO> kladder(@Argument String fnr) {
        var targetFnr = Fnr.of(getContextUserIdent(fnr).get());
        authService.sjekkTilgangTilPerson(targetFnr);
        return kladdService.hentKladder(targetFnr.get())
                .stream()
                .map(kladd -> KladdDTO.builder()
                        .aktivitetId(kladd.getAktivitetId())
                        .dialogId(kladd.getDialogId())
                        .overskrift(kladd.getOverskrift())
                        .tekst(kladd.getTekst())
                        .build())
                .toList();
    }

    private Predicate<DialogData> bareMedAktiviteterFilter(Optional<Boolean> bareMedAktiviteter) {
        return dialog -> {
            if (bareMedAktiviteter.orElse(false)) {
                return dialog.getAktivitetId() != null;
            } else {
                return true;
            }
        };
    }

    private Fnr getContextUserIdent(String fnr) {
        if (authService.erEksternBruker()) {
            return Fnr.of(authService.getLoggedInnUser().get());
        }
        else if (!fnr.isBlank()) {
            return Fnr.of(fnr);
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

}
