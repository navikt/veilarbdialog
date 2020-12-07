package no.nav.fo.veilarbdialog.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SubjectHandler;
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
        IdentType identType = SubjectHandler.getIdentType().orElse(null);

        if(StringUtils.isEmpty(henvendelse.getKontorsperreEnhetId())) {
            return true;
        }

        if(identType == IdentType.EksternBruker) {
            return true;
        }

        return  auth.harVeilederTilgangTilEnhet(auth.getIdent().orElse(null), henvendelse.getKontorsperreEnhetId());
    }


    public boolean filterKontorsperre(DialogData dialog) {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);

        if(StringUtils.isEmpty(dialog.getKontorsperreEnhetId())) {
            return true;
        }

        if(identType == IdentType.EksternBruker) {
            return true;
        }

        return  auth.harVeilederTilgangTilEnhet(auth.getIdent().orElse(null), dialog.getKontorsperreEnhetId());
    }


}
