package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;

import static java.lang.Thread.sleep;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VarselDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";
    private static final long TI_MINUTTER = 1000 * 60 * 10;

    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private VarselDAO varselDAO;

    private long opprettNyDialog(String aktorId) {
        return dialogDAO.opprettDialog(nyDialog(aktorId));
    }

    private long opprettNyDialog() {
        return dialogDAO.opprettDialog(nyDialog(AKTOR_ID));
    }

    @Test
    public void skalIkkeHenteBrukereSomHarBlittVarsletOmUlesteMeldinger() {
        long dialogId = opprettNyDialog();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));

        varselDAO.oppdaterSisteVarselForBruker(AKTOR_ID);

        val aktor = varselDAO.hentAktorerMedUlesteMeldinger(0);
        assertThat(aktor.size(), equalTo(0));
    }

    @Test
    public void skalHenterukereSomHarUlesteMeldingerEtterTidligereVarsel() throws Exception {
        long dialogId = opprettNyDialog();

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));
        varselDAO.oppdaterSisteVarselForBruker(AKTOR_ID);

        sleep(1);

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));

        sleep(1);

        val aktor = varselDAO.hentAktorerMedUlesteMeldinger(0);
        assertThat(aktor.size(), equalTo(1));
    }

    @Test
    public void skalIkkeHentBrukereMedUlesteMeldingerInnenforGracePerioden() throws Exception {
        long dialogId = opprettNyDialog();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));

        sleep(1);

        val aktor1 = varselDAO.hentAktorerMedUlesteMeldinger(0); // Utenfor grace
        assertThat(aktor1.size(), equalTo(1));

        val aktor2 = varselDAO.hentAktorerMedUlesteMeldinger(TI_MINUTTER); // Innenfor grace
        assertThat(aktor2.size(), equalTo(0));
    }

    @Test
    public void skalHenteBrukereMedUlesteMeldinger() throws Exception {
        long dialogId1 = opprettNyDialog("1111");
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId1, AKTOR_ID));

        long dialogId2 = opprettNyDialog("2222");
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId2, AKTOR_ID));

        long dialogId3 = opprettNyDialog("3333");
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId3, AKTOR_ID));

        long dialogId4 = opprettNyDialog("4444");
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId4, AKTOR_ID));

        sleep(1L);

        val aktorer = varselDAO.hentAktorerMedUlesteMeldinger(0);

        assertThat(aktorer, IsIterableContainingInAnyOrder.containsInAnyOrder("1111", "2222", "3333", "4444"));
    }

}