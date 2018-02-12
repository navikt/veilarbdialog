package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.service.MetadataService;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;

import static java.lang.Thread.sleep;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VarselDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";
    private static final long TI_MINUTTER = 1000 * 60 * 10;

    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private MetadataService metadataService;

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
        HenvendelseData henvendelseData = getHenvendelseData(dialogId);
        dialogDAO.opprettHenvendelse(henvendelseData);

        varselDAO.oppdaterSisteVarselForBruker(AKTOR_ID);

        val aktor = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0);
        assertThat(aktor.size(), equalTo(0));
    }

    @Test
    public void skalHenterukereSomHarUlesteMeldingerEtterTidligereVarsel() throws Exception {
        long dialogId = opprettNyDialog();

        dialogDAO.opprettHenvendelse(getHenvendelseData(dialogId));
        varselDAO.oppdaterSisteVarselForBruker(AKTOR_ID);

        sleep(1);

        dialogDAO.opprettHenvendelse(getHenvendelseData(dialogId));

        sleep(1);

        val aktor = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0);
        assertThat(aktor.size(), equalTo(1));
    }

    @Test
    public void skalIkkeHentBrukereMedUlesteMeldingerInnenforGracePerioden() throws Exception {
        long dialogId = opprettNyDialog();
        dialogDAO.opprettHenvendelse(getHenvendelseData(dialogId));

        sleep(1);

        val aktor1 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0); // Utenfor grace
        assertThat(aktor1.size(), equalTo(1));

        val aktor2 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(TI_MINUTTER); // Innenfor grace
        assertThat(aktor2.size(), equalTo(0));
    }

    @Test
    public void skalHenteBrukereMedUlesteMeldinger() throws Exception {
        long dialogId1 = opprettNyDialog("1111");
        HenvendelseData henvendelseData1 = getHenvendelseData(dialogId1);
        dialogDAO.opprettHenvendelse(henvendelseData1);

        long dialogId2 = opprettNyDialog("2222");
        HenvendelseData henvendelseData2 = getHenvendelseData(dialogId2);
        dialogDAO.opprettHenvendelse(henvendelseData2);

        long dialogId3 = opprettNyDialog("3333");
        HenvendelseData henvendelseData3 = getHenvendelseData(dialogId3);
        dialogDAO.opprettHenvendelse(henvendelseData3);

        long dialogId4 = opprettNyDialog("4444");
        HenvendelseData henvendelseData4 = getHenvendelseData(dialogId4);
        dialogDAO.opprettHenvendelse(henvendelseData4);

        sleep(1L);

        val aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0);

        assertThat(aktorer, IsIterableContainingInAnyOrder.containsInAnyOrder("1111", "2222", "3333", "4444"));
    }

    @Test
    public void skalIkkeSendeVarselForHenvendelserSomerLagtInnAvBrukerenSelv() throws Exception {
        long dialogId = opprettNyDialog();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID, AvsenderType.BRUKER).withSendt(new Date()));

        sleep(1);

        val aktor1 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0); // Utenfor grace
        assertThat(aktor1.size(), equalTo(0));

    }

    private HenvendelseData getHenvendelseData(long dialogId) {
        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID, AvsenderType.VEILEDER).withSendt(new Date());
        DialogData dialogData = dialogDAO.hentDialog(dialogId);
        metadataService.nyHenvendelse(dialogData, henvendelseData);
        return henvendelseData;
    }
}