package no.nav.fo.veilarbdialog.service;


import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbdialog.db.dao.KladdDAO;
import no.nav.fo.veilarbdialog.domain.Kladd;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarbdialog.service.AutorisasjonService.erEksternBruker;

@Component
public class KladdService {

    private KladdDAO kladdDAO;

    private AktorService aktorService;

    private final int EN_TIME = 1;

    @Inject
    public KladdService(KladdDAO kladdDAO, AktorService aktorService){
        this.kladdDAO = kladdDAO;
        this.aktorService = aktorService;
    }

    public List<Kladd> hentKladder(String fnr){
        String aktorId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new RuntimeException("Mangler aktor id"));

        if (erEksternBruker()){
            return kladdDAO.getKladder(aktorId, aktorId);
        }

        return SubjectHandler
                .getIdent()
                .map(ident -> kladdDAO.getKladder(aktorId, ident))
                .orElse(Collections.emptyList());
    }

    public void upsertKladd(String fnr, Kladd kladd){
        String aktorId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new RuntimeException("Mangler aktor id"));

        Kladd kladdWithUserContext = addUserContext(aktorId, kladd);
        kladdDAO.upsertKladd(kladdWithUserContext);
    }

    public void slettGamleKladder() {
        kladdDAO.slettKladderGamlereEnnTimer(EN_TIME);
    }

    private Kladd addUserContext(String aktorId, Kladd kladd) {
        if (erEksternBruker()) {
            return kladd.withAktorId(aktorId).withLagtInnAv(aktorId);
        }

        return kladd.withAktorId(aktorId).withLagtInnAv(getLoggedInUserIdent());
    }

    public void deleteKladd(String fnr, String dialogId, String aktivitetId){
        String aktorId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new RuntimeException("Mangler aktor id"));

        Kladd kladd = Kladd.builder()
                .aktivitetId(aktivitetId)
                .dialogId(dialogId)
                .build();

        Kladd kladdWithUserContext = addUserContext(aktorId, kladd);
        kladdDAO.slettKladd(kladdWithUserContext);
    }

    private String getLoggedInUserIdent() {
        return SubjectHandler.getIdent().orElse("SYSTEM");
    }
}
