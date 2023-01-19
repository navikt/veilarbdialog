package no.nav.fo.veilarbdialog.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.text.ParseException;
import java.util.Objects;
import java.util.Optional;

import static no.nav.fo.veilarbdialog.util.AuthUtils.erSystemkallFraAzureAd;

@Service
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthService {

    private final Pep pep;
    private final AuthContextHolder authContextHolder;

    public Optional<String> getIdent() {
        return authContextHolder.getUid();
    }

    private boolean eksternBrukerHasNiva4() {
        return authContextHolder.getIdTokenClaims()
            .map(jwtClaimsSet -> {
                try {
                    return Objects.equals(jwtClaimsSet.getStringClaim("acr"), "Level4");
                } catch (ParseException e) {
                    return false;
                }
            }).orElse(false);
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

    public void sjekkEksternBrukerHarTilgang(Fnr fnr) {
        var loggedInUserFnr = getIdent().orElse("");
        if (!loggedInUserFnr.equals(fnr.get())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ekstern bruker har ikke tilgang til andre brukere enn seg selv"
            );
        }
        if (!eksternBrukerHasNiva4()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ekstern bruker har ikke innloggingsnivå 4"
            );
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

    public boolean erSystemBruker() {
        return authContextHolder.erSystemBruker();
    }

    public boolean erInternBruker() {
        return authContextHolder.erInternBruker();
    }

    public String getInnloggetBrukerToken() {
        return authContextHolder.requireIdTokenString();
    }

}
