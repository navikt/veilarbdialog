package no.nav.fo.veilarbdialog.ws.provider;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.HentDialogerForBrukerRequest;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.OpprettDialogForAktivitetsplanRequest;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.TestData.KJENT_IDENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class SoapServiceTest extends IntegrasjonsTest {

    @Inject
    private SoapService soapService;

    @Test
    public void opprettOgHentDialoger() throws Exception {
        soapService.opprettDialogForAktivitetsplan(opprettDialogForAktivitetsplanRequest());
        val hentAktiviteterResponse = soapService.hentDialogerForBruker(hentDialogerForBrukerRequest());
        assertThat(hentAktiviteterResponse.getDialogListe(), hasSize(1));
    }

    private OpprettDialogForAktivitetsplanRequest opprettDialogForAktivitetsplanRequest() {
        val opprettDialogForAktivitetsplanRequest = new OpprettDialogForAktivitetsplanRequest();
        opprettDialogForAktivitetsplanRequest.setTittel("tittel");
        opprettDialogForAktivitetsplanRequest.setPersonIdent(KJENT_IDENT);
        return opprettDialogForAktivitetsplanRequest;
    }

    private HentDialogerForBrukerRequest hentDialogerForBrukerRequest() {
        val hentAktiviteterRequest = new HentDialogerForBrukerRequest();
        hentAktiviteterRequest.setPersonIdent(KJENT_IDENT);
        return hentAktiviteterRequest;
    }

}