package no.nav.fo.veilarbdialog.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.types.feil.IngenTilgang;
import no.nav.fo.veilarbdialog.domain.KontorsperreEnhetData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.nav.common.auth.subject.IdentType.EksternBruker;
import static no.nav.common.auth.subject.IdentType.InternBruker;

@Service
@RequiredArgsConstructor
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
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        if (identType != InternBruker) {
            throw new IngenTilgang(String.format("%s != %s", identType, InternBruker));
        }
    }

    public static boolean erEksternBruker() {
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return EksternBruker.equals(type);
    }

    public static boolean erInternBruker() {
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return InternBruker.equals(type);
    }

    public boolean filterKontorsperre(KontorsperreEnhetData kontorsperreEnhetData) {
        if (StringUtils.isEmpty(kontorsperreEnhetData.getKontorsperreEnhetId())) {
            return true;
        }
        if (erEksternBruker()) {
            return true;
        }
        return pep.harVeilederTilgangTilEnhet(getIdent().orElse(null), kontorsperreEnhetData.getKontorsperreEnhetId());
    }

}
