package no.nav.fo.veilarbdialog.ws.provider;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.tjeneste.domene.brukerdialog.dialogoppfoelging.v1.meldinger.HentDialogerForBrukerRequest;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.TestData.KJENT_IDENT;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class SoapServiceTest extends IntegrasjonsTest {

    @Inject
    private SoapService soapService;

    @Inject
    private DialogDAO dialogDAO;

    @Test
    public void hent_aktiviteter() throws Exception {
        opprett_aktivitet();
        val hentAktiviteterResponse = soapService.hentDialogerForBruker(getHentAktivitetsplanRequest());
        assertThat(hentAktiviteterResponse.getDialogListe(), hasSize(1));
    }

//    @Test
//    public void opprett_aktiviteter() throws Exception {
//        val opprettNyAktivitetRequest = getOpprettNyAktivitetRequest();
//        val beskrivelse = "Batman er awesome!!!!!";
//        opprettNyAktivitetRequest.getAktivitet().setBeskrivelse(beskrivelse);
//        soapService.opprettNyAktivitet(opprettNyAktivitetRequest);
//
//        val aktiviter = aktiviter();
//        assertThat(aktiviter, hasSize(1));
//        assertThat(aktiviter.get(0).getBeskrivelse(), containsString(beskrivelse));
//    }
//
//    @Test
//    public void slett_aktivitet() throws Exception {
//        opprett_aktivitet();
//        val aktivitetId = Long.toString(aktiviter().get(0).getId());
//
//        assertThat(aktiviter(), hasSize(1));
//
//        val slettReq = new SlettAktivitetRequest();
//        slettReq.setAktivitetId(aktivitetId);
//        soapService.slettAktivitet(slettReq);
//
//        assertThat(aktiviter(), empty());
//    }
//
//    @Test
//    public void endre_aktivitet_status() throws Exception {
//        opprett_aktivitet();
//
//        val aktivitetId = Long.toString(aktiviter().get(0).getId());
//        val endreReq = new EndreAktivitetStatusRequest();
//        endreReq.setAktivitetId(aktivitetId);
//        endreReq.setStatus(Status.GJENNOMFOERT);
//
//        val res1 = soapService.endreAktivitetStatus(endreReq);
//        assertThat(res1.getAktivitet().getStatus(), equalTo(Status.GJENNOMFOERT));
//
//
//        endreReq.setStatus(Status.AVBRUTT);
//        val res2 = soapService.endreAktivitetStatus(endreReq);
//        assertThat(res2.getAktivitet().getStatus(), equalTo(Status.AVBRUTT));
//    }
//
//    @Test
//    public void hent_endringslogg() throws Exception {
//        opprett_aktivitet();
//
//        val aktivitetId = Long.toString(aktiviter().get(0).getId());
//        val endreReq = new EndreAktivitetStatusRequest();
//        endreReq.setAktivitetId(aktivitetId);
//        endreReq.setStatus(Status.GJENNOMFOERT);
//
//        soapService.endreAktivitetStatus(endreReq);
//
//        val req = new HentEndringsLoggForAktivitetRequest();
//        req.setAktivitetId(aktivitetId);
//        assertThat(soapService.hentEndringsLoggForAktivitet(req).getEndringslogg(), hasSize(1));
//    }


//    private List<DialogData> aktiviter() throws Exception {
//        return dialogDAO.hentDialogerForAktorId(KJENT_AKTOR_ID);
//    }

    private void opprett_aktivitet() {
        dialogDAO.opprettDialog(nyDialog(KJENT_AKTOR_ID));
    }

//    private OpprettNyAktivitetRequest getOpprettNyAktivitetRequest() {
//        OpprettNyAktivitetRequest opprettNyAktivitetRequest = new OpprettNyAktivitetRequest();
//
//        val aktivitet = nyAktivitetWS();
//        aktivitet.setEgenAktivitet(new Egenaktivitet());
//
//        opprettNyAktivitetRequest.setAktivitet(aktivitet);
//        return opprettNyAktivitetRequest;
//    }

    private HentDialogerForBrukerRequest getHentAktivitetsplanRequest() {
        val hentAktiviteterRequest = new HentDialogerForBrukerRequest();
        hentAktiviteterRequest.setPersonIdent(KJENT_IDENT);
        return hentAktiviteterRequest;
    }


}