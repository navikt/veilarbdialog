package no.nav.fo.veilarbdialog.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.types.feil.IngenTilgang;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthService {

    private final Pep pep;

    public String getSsoToken()
            throws IngenTilgang {
        return SubjectHandler
                .getSsoToken()
                .orElseThrow(
                        () -> new IngenTilgang(String.format(
                                "No SSO token found for ident %s",
                                SubjectHandler
                                        .getIdent()
                                        .orElse("unknown")))
                )
                .getToken();
    }

    public Optional<String> getIdent() {
        return SubjectHandler.getIdent();
    }

    public boolean harVeilederTilgangTilEnhet(String ident, String enhet) {
        return pep.harVeilederTilgangTilEnhet(ident, enhet);
    }

    public boolean harVeilederTilgangTilPerson(String ident, String aktorId) {
        return pep.harVeilederTilgangTilPerson(
                ident,
                ActionId.READ,
                AbacPersonId.aktorId(aktorId)
        );
    }

    public boolean harTilgangTilPerson(String aktorId) {
        return pep.harTilgangTilPerson(
                getSsoToken(),
                ActionId.READ,
                AbacPersonId.aktorId(aktorId)
        );
    }

    public void harTilgangTilPersonEllerKastIngenTilgang(String aktorId)
            throws IngenTilgang {
        if (!harTilgangTilPerson(aktorId)) {
            throw new IngenTilgang(String.format(
                    "%s har ikke lesetilgang til %s",
                    getIdent().orElse("null"),
                    aktorId
            ));
        }
    }

    public void skalVereInternBruker() {
        skalVere(IdentType.InternBruker);
    }

    private void skalVere(IdentType forventetIdentType) {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        if (identType != forventetIdentType) {
            throw new IngenTilgang(String.format("%s != %s", identType, forventetIdentType));
        }
    }

    public static boolean erEksternBruker() {
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return IdentType.EksternBruker.equals(type);
    }

    public static boolean erInternBruker() {
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return IdentType.InternBruker.equals(type);
    }

}
