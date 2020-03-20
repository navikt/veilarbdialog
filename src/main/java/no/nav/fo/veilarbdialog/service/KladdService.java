package no.nav.fo.veilarbdialog.service;


import no.nav.fo.veilarbdialog.db.dao.KladdDAO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class KladdService {

    private KladdDAO kladdDAO;

    @Inject
    public KladdService(KladdDAO kladdDAO){
        this.kladdDAO = kladdDAO;
    }


}
