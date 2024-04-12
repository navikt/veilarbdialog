package no.nav.fo.veilarbdialog.graphql;

import lombok.AllArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.Person;
import no.nav.fo.veilarbdialog.rest.RestMapper;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.poao.dab.spring_auth.AuthService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Controller
public class DialogGraphqlController {

    final private AuthService authService;
    final private DialogDataService dialogDataService;
    final private RestMapper restMapper;

    @QueryMapping
    public List<DialogDTO> dialoger(@Argument String fnr) {
        var targetFnr = Fnr.of(getContextUserIdent(fnr).get());
        authService.sjekkTilgangTilPerson(targetFnr);
        return dialogDataService.hentDialogerForBruker(Person.fnr(fnr))
                .stream().map(restMapper::somDialogDTO).toList();
    }

    private Fnr getContextUserIdent(String fnr) {
        if (authService.erEksternBruker()) {
            return Fnr.of(authService.getLoggedInnUser().get());
        }
        else if (!fnr.isBlank()) {
            return Fnr.of(fnr);
        }
        else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

}
