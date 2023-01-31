package no.nav.fo.veilarbdialog.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EnhetId;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KontorsperreFilter {

    private final IAuthService auth;

    public boolean tilgangTilEnhet(NoeMedKontorEnhet ting) {
        return ting.getKontorEnhet()
                .map(enhetId -> tilgangTilEnhet(EnhetId.of(enhetId.get())))
                .orElse(true);
    }
    public boolean tilgangTilEnhet(EnhetId enhetId) {
        if(StringUtils.isEmpty(enhetId.get())) return true;
        if(auth.erEksternBruker()) return true;
        return  auth.harTilgangTilEnhet(enhetId);
    }
}

