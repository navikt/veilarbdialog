package no.nav.fo.veilarbdialog.kvp;

import static no.nav.apiapp.util.StringUtils.nullOrEmpty;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import no.nav.apiapp.security.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

@Component
public class KontorsperreFilter {

    @Inject
    PepClient pepClient;
    
    public boolean harTilgang(String enhet) {
        try {
            return nullOrEmpty(enhet) || pepClient.harTilgangTilEnhet(enhet);
        } catch (PepException e) {
            return false;
        }
    }

}
