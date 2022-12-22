package no.nav.fo.veilarbdialog.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static no.nav.fo.veilarbdialog.util.AuthUtils.erSystemkallFraAzureAd;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthService {

    private final Pep pep;
    private final AuthContextHolder authContextHolder;

    public Optional<String> getIdent() {
        return authContextHolder.getUid();
    }

    public NavIdent getNavIdent() {
        if (erInternBruker()) {
            return authContextHolder.getNavIdent().orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Kan ikke hente navident."));
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke pålogget som internbruker");
        }
    }

    public boolean harVeilederTilgangTilEnhet(String ident, String enhet) {
        return pep.harVeilederTilgangTilEnhet(NavIdent.of(ident), EnhetId.of(enhet));
    }

    public boolean harVeilederTilgangTilPerson(String ident, String aktorId) {
        return pep.harVeilederTilgangTilPerson(
                NavIdent.of(ident),
                ActionId.READ,
                AktorId.of(aktorId)
        );
    }

    public boolean harTilgangTilPerson(String aktorId) {
        if (erSystemkallFraAzureAd(authContextHolder)) {
            return true;
        } else {
            return pep.harTilgangTilPerson(
                getInnloggetBrukerToken(),
                ActionId.READ,
                AktorId.of(aktorId)
            );
        }
    }

    public void harTilgangTilPersonEllerKastIngenTilgang(Fnr fnr) {
        if (!pep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, fnr)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format(
                "%s har ikke lesetilgang til person",
                getIdent().orElse("null")
            ));
        }
    }

    public void harTilgangTilPersonEllerKastIngenTilgang(String aktorId) {
        if (!harTilgangTilPerson(aktorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format(
                    "%s har ikke lesetilgang til %s",
                    getIdent().orElse("null"),
                    aktorId
            ));
        }
    }

    public void skalVereInternBruker() {
        if (!authContextHolder.erInternBruker()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig bruker type");
        }
    }

    public boolean erEksternBruker() {
        return authContextHolder.erEksternBruker();
    }

    public boolean erInternBruker() {
        return authContextHolder.erInternBruker();
    }

    public String getInnloggetBrukerToken() {
        return authContextHolder.getIdTokenString()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fant ikke token for innlogget bruker"));
    }

}
