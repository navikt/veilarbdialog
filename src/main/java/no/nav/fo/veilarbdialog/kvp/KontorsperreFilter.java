package no.nav.fo.veilarbdialog.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.auth.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KontorsperreFilter {

    private final AuthService auth;

    // TODO: When bumping commons-java, the harVeilederTilgangTilEnhet(...) will change behaviour.
    public boolean harTilgang(String ident, String enhet) {
        return StringUtils.isEmpty(enhet) || auth.harVeilederTilgangTilEnhet(ident, enhet);
    }

}
