package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.db.dao.KladdDAO;
import no.nav.fo.veilarbdialog.domain.Kladd;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KladdService {

    private final KladdDAO kladdDAO;
    private final AktorOppslagClient aktorOppslagClient;
    private final IAuthService auth;

    public List<Kladd> hentKladder(String fnr) {
        String aktorId = aktorOppslagClient.hentAktorId(Fnr.of(fnr)).get();
        if (auth.erEksternBruker()) {
            return kladdDAO.getKladder(aktorId, aktorId);
        }
        var ident =  auth.getLoggedInnUser();
        return kladdDAO.getKladder(aktorId, ident.get());
    }

    public void upsertKladd(String fnr, Kladd kladd) {
        String aktorId = aktorOppslagClient.hentAktorId(Fnr.of(fnr)).get();
        Kladd kladdWithUserContext = addUserContext(aktorId, kladd);
        kladdDAO.upsertKladd(kladdWithUserContext);
    }

    public void slettGamleKladder() {
        kladdDAO.slettKladderGamlereEnnTimer(24);
    }

    private Kladd addUserContext(String aktorId, Kladd kladd) {
        if (auth.erEksternBruker()) {
            return kladd.withAktorId(aktorId).withLagtInnAv(aktorId);
        }
        return kladd.
                withAktorId(aktorId)
                .withLagtInnAv(auth.getLoggedInnUser().get());
    }

    public void deleteKladd(String fnr, String dialogId, String aktivitetId) {
        String aktorId = aktorOppslagClient.hentAktorId(Fnr.of(fnr)).get();
        Kladd kladd = Kladd.builder()
                .aktivitetId(aktivitetId)
                .dialogId(dialogId)
                .build();
        Kladd kladdWithUserContext = addUserContext(aktorId, kladd);
        kladdDAO.slettKladd(kladdWithUserContext);
    }

}
