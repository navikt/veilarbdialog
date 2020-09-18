package no.nav.fo.veilarbdialog.service;

import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.types.feil.IngenTilgang;
import org.springframework.stereotype.Service;

@Service
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

    public static boolean erEksternBruker() {
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return IdentType.EksternBruker.equals(type);
    }

    public static boolean erInternBruker() {
        IdentType type = SubjectHandler.getIdentType().orElse(null);
        return IdentType.InternBruker.equals(type);
    }

}
