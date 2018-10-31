package no.nav.fo.veilarbdialog.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SubjectHandler;
import org.springframework.stereotype.Component;

@Component
public class AutorisasjonService {

    public void skalVereInternBruker() {
        skalVere(IdentType.InternBruker);
    }

    private void skalVere(IdentType forventetIdentType) {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        if (identType != forventetIdentType) {
            throw new IngenTilgang(String.format("%s != %s", identType, forventetIdentType));
        }
    }

    public static boolean erEksternBruker(){
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return IdentType.EksternBruker.equals(type);
    }

    public static boolean erInternBruker(){
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return IdentType.InternBruker.equals(type);
    }

}