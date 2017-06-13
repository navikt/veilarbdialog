package no.nav.fo.veilarbdialog.rest;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectHandler;
//import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PepClient {

    @Inject
    private Pep pep;

    @SneakyThrows
    public String sjekkTilgangTilFnr(String fnr) {
        if (Decision.Permit == pep.isServiceCallAllowedWithOidcToken(getToken(), "veilarb", fnr).getBiasedDecision()) {
            return fnr;
        } else {
            throw new IngenTilgang();
        }
    }

    private String getToken() {
        throw new IngenTilgang();
//        return SubjectHandler.getSubjectHandler().getSubject()
//                .getPublicCredentials()
//                .stream()
////                .filter(o -> o instanceof OidcCredential)
////                .map(o -> (OidcCredential) o)
//                .findFirst()
////                .map(OidcCredential::getToken)
//                .orElseThrow(IngenTilgang::new);
    }

}
