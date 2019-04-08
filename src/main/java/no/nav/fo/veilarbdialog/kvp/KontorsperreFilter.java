package no.nav.fo.veilarbdialog.kvp;

import lombok.SneakyThrows;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.apiapp.util.StringUtils.nullOrEmpty;

@Component
public class KontorsperreFilter {

    @Inject
    VeilarbAbacPepClient pepClient;

    @SneakyThrows
    public boolean harTilgang(String enhet) {
        return nullOrEmpty(enhet) || pepClient.harTilgangTilEnhet(enhet);
    }

}
