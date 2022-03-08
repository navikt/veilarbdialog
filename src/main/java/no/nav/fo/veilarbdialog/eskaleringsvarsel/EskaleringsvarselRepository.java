package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class EskaleringsvarselRepository {

    public void opprett(long tilhorendeDialogId, String aktorId, String opprettetAv, String opprettetBegrunnelse) {

    }

    public void stopp(long varselId, String avsluttetAv, String avsluttetBegrunnelse) {

    }

    public EskaleringsvarselEntity hentGjeldende(AktorId aktorId) {
        return null;
    }

    public List<EskaleringsvarselEntity> hentHistorikk(AktorId aktorId) {
        return Collections.emptyList();
    }

}
