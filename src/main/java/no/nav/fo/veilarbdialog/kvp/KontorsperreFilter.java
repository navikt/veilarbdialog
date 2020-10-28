package no.nav.fo.veilarbdialog.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KontorsperreFilter {

    private final AuthService auth;

    public boolean harTilgang(String ident, String enhet) {
        return StringUtils.isEmpty(enhet) || auth.identifiedUserHasReadAccessToEnhet(ident, enhet);
    }

}
