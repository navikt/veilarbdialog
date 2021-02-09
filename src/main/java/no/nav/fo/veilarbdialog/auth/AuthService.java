package no.nav.fo.veilarbdialog.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthService {

    private final Pep pep;

    public Optional<String> getIdent() {
        if (erInternBruker()) {
            return AuthContextHolder.getNavIdent().map(NavIdent::get);
        } else {
            return AuthContextHolder.getSubject();
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
        return pep.harTilgangTilPerson(
                getInnloggetBrukerToken(),
                ActionId.READ,
                AktorId.of(aktorId)
        );
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
        if (!AuthContextHolder.erInternBruker()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig bruker type");
        };
    }

    public static boolean erEksternBruker() {
        return AuthContextHolder.erEksternBruker();
    }

    public static boolean erInternBruker() {
        return AuthContextHolder.erInternBruker();
    }

    public String getInnloggetBrukerToken() {
        return AuthContextHolder.getIdTokenString()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fant ikke token for innlogget bruker"));
    }

}
