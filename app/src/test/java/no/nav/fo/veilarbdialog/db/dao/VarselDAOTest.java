package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VarselDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";
    private static final long TI_MINUTTER = 1000 * 60 * 10;
    private static final Date EN_DAG_SIDEN = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
    private static final Date EN_TIME_SIDEN = new Date(System.currentTimeMillis() - 1000 * 60 * 60);
    private static final Date TI_MINUTT_SIDEN = new Date(System.currentTimeMillis() - TI_MINUTTER);
    private static final Date ET_MINUTT_SIDEN = new Date(System.currentTimeMillis() - 1000 * 60);


    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private VarselDAO varselDAO;

    private long opprettNyDialog(String aktorId, Date date) {
        return dialogDAO.opprettDialog(nyDialog(aktorId), date);
    }

    private long opprettNyDialog(Date date) {
        return dialogDAO.opprettDialog(nyDialog(AKTOR_ID), date);
    }


    @Test
    public void skalIkkeHenteBrukereSomHarBlittVarsletOmUlesteMeldinger() {
        long dialogId = opprettNyDialog(EN_DAG_SIDEN);
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID), EN_TIME_SIDEN);

        varselDAO.oppdaterSisteVarselForBruker(AKTOR_ID, TI_MINUTT_SIDEN);

        val aktor = varselDAO.hentAktorerMedUlesteMeldinger(0);
        assertThat(aktor.size(), equalTo(0));
    }

    @Test
    public void skalHenterukereSomHarUlesteMeldingerEtterTidligereVarsel() {
        long dialogId = opprettNyDialog(EN_DAG_SIDEN);

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID), EN_TIME_SIDEN);
        varselDAO.oppdaterSisteVarselForBruker(AKTOR_ID, TI_MINUTT_SIDEN);

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID), ET_MINUTT_SIDEN);

        val aktor = varselDAO.hentAktorerMedUlesteMeldinger(0);
        assertThat(aktor.size(), equalTo(1));
    }

    @Test
    public void skalIkkeHentBrukereMedUlesteMeldingerInnenforGracePerioden() {
        long dialogId = opprettNyDialog(EN_DAG_SIDEN);
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID), ET_MINUTT_SIDEN);

        val aktor1 = varselDAO.hentAktorerMedUlesteMeldinger(0); // Utenfor grace
        assertThat(aktor1.size(), equalTo(1));

        val aktor2 = varselDAO.hentAktorerMedUlesteMeldinger(TI_MINUTTER); // Innenfor grace
        assertThat(aktor2.size(), equalTo(0));
    }

    @Test
    public void skalHenteBrukereMedUlesteMeldinger() {
        long dialogId1 = opprettNyDialog("1111", EN_TIME_SIDEN);
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId1, AKTOR_ID), ET_MINUTT_SIDEN);

        long dialogId2 = opprettNyDialog("2222", EN_TIME_SIDEN);
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId2, AKTOR_ID), ET_MINUTT_SIDEN);

        long dialogId3 = opprettNyDialog("3333", EN_TIME_SIDEN);
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId3, AKTOR_ID), ET_MINUTT_SIDEN);

        long dialogId4 = opprettNyDialog("4444", EN_TIME_SIDEN);
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId4, AKTOR_ID), ET_MINUTT_SIDEN);


        val aktorer = varselDAO.hentAktorerMedUlesteMeldinger(0);

        assertThat(aktorer, IsIterableContainingInAnyOrder.containsInAnyOrder("1111", "2222", "3333", "4444"));
    }
}