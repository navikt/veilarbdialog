package no.nav.fo.veilarbdialog.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KontorsperreFilter {

    private final AuthService auth;

    public boolean filterKontorsperre(HenvendelseData henvendelse) {
        if(StringUtils.isEmpty(henvendelse.getKontorsperreEnhetId())) {
            return true;
        }
        if(AuthService.erEksternBruker()) {
            return true;
        }

        return  auth.harVeilederTilgangTilEnhet(auth.getIdent().orElse(null), henvendelse.getKontorsperreEnhetId());
    }


    public boolean filterKontorsperre(DialogData dialog) {
        if(StringUtils.isEmpty(dialog.getKontorsperreEnhetId())) {
            return true;
        }

        if(AuthService.erEksternBruker()) {
            return true;
        }

        return  auth.harVeilederTilgangTilEnhet(auth.getIdent().orElse(null), dialog.getKontorsperreEnhetId());
    }
}
