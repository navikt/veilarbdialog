package no.nav.fo.veilarbdialog.kvp;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.abac.Pep;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KontorsperreFilter {

    private final Pep pep;

    @SneakyThrows
    public boolean harTilgang(String ident, String enhet) {
        return StringUtils.isEmpty(enhet) || pep.harVeilederTilgangTilEnhet(ident, enhet);
    }

}
