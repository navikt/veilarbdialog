package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.fo.veilarbdialog.db.dao.KladdDAO;
import no.nav.fo.veilarbdialog.domain.Kladd;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarbdialog.service.AuthService.erEksternBruker;

@Service
@RequiredArgsConstructor
public class KladdService {

    private final KladdDAO kladdDAO;
    private final AktorregisterClient aktorregister;
    private final AuthService auth;

    public List<Kladd> hentKladder(String fnr) {

        String aktorId = aktorregister.hentAktorId(fnr);
        if (erEksternBruker()) {
            return kladdDAO.getKladder(aktorId, aktorId);
        }
        return SubjectHandler
                .getIdent()
                .map(ident -> kladdDAO.getKladder(aktorId, ident))
                .orElse(Collections.emptyList());

    }

    public void upsertKladd(String fnr, Kladd kladd) {

        String aktorId = aktorregister.hentAktorId(fnr);
        Kladd kladdWithUserContext = addUserContext(aktorId, kladd);
        kladdDAO.upsertKladd(kladdWithUserContext);

    }

    public void slettGamleKladder() {
        kladdDAO.slettKladderGamlereEnnTimer(24);
    }

    private Kladd addUserContext(String aktorId, Kladd kladd) {

        if (erEksternBruker()) {
            return kladd.withAktorId(aktorId).withLagtInnAv(aktorId);
        }
        return kladd.withAktorId(aktorId).withLagtInnAv(auth.getIdent().orElse("SYSTEM"));

    }

    public void deleteKladd(String fnr, String dialogId, String aktivitetId) {

        String aktorId = aktorregister.hentAktorId(fnr);
        Kladd kladd = Kladd.builder()
                .aktivitetId(aktivitetId)
                .dialogId(dialogId)
                .build();
        Kladd kladdWithUserContext = addUserContext(aktorId, kladd);
        kladdDAO.slettKladd(kladdWithUserContext);

    }

}
