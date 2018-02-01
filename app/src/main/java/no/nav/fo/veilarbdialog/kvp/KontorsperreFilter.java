package no.nav.fo.veilarbdialog.kvp;

import static no.nav.apiapp.util.StringUtils.nullOrEmpty;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import no.nav.apiapp.security.PepClient;

@Component
public class KontorsperreFilter {

    @Inject
    PepClient pepClient;
    
    @SneakyThrows
    public boolean harTilgang(String enhet) {
        return nullOrEmpty(enhet) || pepClient.harTilgangTilEnhet(enhet);
    }

}
