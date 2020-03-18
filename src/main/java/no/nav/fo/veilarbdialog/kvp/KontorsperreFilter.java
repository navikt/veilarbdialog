package no.nav.fo.veilarbdialog.kvp;

import lombok.SneakyThrows;
import no.nav.apiapp.security.PepClient;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.apiapp.util.StringUtils.nullOrEmpty;

@Component
public class KontorsperreFilter {

    @Inject
    PepClient pepClient;

    @SneakyThrows
    public boolean harTilgang(String enhet) {
        return nullOrEmpty(enhet) || pepClient.harTilgangTilEnhet(enhet);
    }

}
