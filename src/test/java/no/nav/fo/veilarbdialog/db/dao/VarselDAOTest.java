package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.service.DialogStatusService;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.Date;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
@ActiveProfiles("local")
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = BEFORE_TEST_METHOD
)
class VarselDAOTest {

    private static final String AKTOR_ID = "1234";
    private static final long TI_MINUTTER = 1000 * 60 * 10;

    @Autowired
    private DialogDAO dialogDAO;

    @Autowired
    private DialogStatusService dialogStatusService;

    @Autowired
    private VarselDAO varselDAO;

    private DialogData opprettNyDialog(String aktorId) {
        return dialogDAO.opprettDialog(nyDialog(aktorId));
    }

    private DialogData opprettNyDialog() {
        return dialogDAO.opprettDialog(nyDialog(AKTOR_ID));
    }

    @Test
    void skalIkkeHenteBrukereSomHarBlittVarsletOmUlesteMeldinger() {
        DialogData dialogId = opprettNyDialog();
        HenvendelseData henvendelseData = getHenvendelseData(dialogId, new Date());
        dialogDAO.opprettHenvendelse(henvendelseData);

        varselDAO.oppdaterSisteVarselForBruker(AKTOR_ID);

        val aktor = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0);
        assertThat(aktor.size(), equalTo(0));
    }

    @Test
    void hentAktorerMedUlesteMeldingerEtterSisteVarsel_returnererIkkeDeUtenforGraceperiode() throws Exception {
        DialogData dialogId = opprettNyDialog();
        dialogDAO.opprettHenvendelse(getHenvendelseData(dialogId, getNowMinusSeconds(10)));

        val aktor1 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0); // Utenfor grace
        assertThat(aktor1.size(), equalTo(1));

        val aktor2 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(TI_MINUTTER); // Innenfor grace
        assertThat(aktor2.size(), equalTo(0));
    }

    @Test
    void skalHenteBrukereMedUlesteMeldinger() {
        DialogData dialogId1 = opprettNyDialog("1111");
        HenvendelseData henvendelseData1 = getHenvendelseData(dialogId1, getNowMinusSeconds(30));
        ;
        dialogDAO.opprettHenvendelse(henvendelseData1);

        DialogData dialogId2 = opprettNyDialog("2222");
        HenvendelseData henvendelseData2 = getHenvendelseData(dialogId2, getNowMinusSeconds(20));
        dialogDAO.opprettHenvendelse(henvendelseData2);


        val aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0);

        assertThat(aktorer, IsIterableContainingInAnyOrder.containsInAnyOrder("1111", "2222"));
    }

    @Test
    void skalIkkeSendeVarselForHenvendelserSomerLagtInnAvBrukerenSelv() throws Exception {
        DialogData dialogId = opprettNyDialog();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId.getId(), AKTOR_ID, AvsenderType.BRUKER).withSendt(getNowMinusSeconds(30)));

        val aktor1 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0); // Utenfor grace
        assertThat(aktor1.size(), equalTo(0));

    }

    private HenvendelseData getHenvendelseData(DialogData dialogData, Date sendt) {
        HenvendelseData henvendelseData = nyHenvendelse(dialogData.getId(), AKTOR_ID, AvsenderType.VEILEDER).withSendt(sendt);
        dialogStatusService.nyHenvendelse(dialogData, henvendelseData);
        return henvendelseData;
    }

    private Date getNowMinusSeconds(int seconds) {
        return Date.from(Instant.now().minusSeconds(seconds));
    }

}
