package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class VarselDAOTest extends BaseDAOTest {

    private static final long TI_MINUTTER = 1000 * 60 * 10;

    private static DialogDAO dialogDAO;

    private static VarselDAO varselDAO;

    @BeforeAll
    public static void setup() {
        dialogDAO = new DialogDAO(jdbc);
        varselDAO = new VarselDAO(jdbc);
    }

    private DialogData opprettNyDialog(String aktorId) {
        return dialogDAO.opprettDialog(nyDialog(aktorId));
    }

    @Test
    void skalIkkeHenteBrukereSomHarBlittVarsletOmUlesteMeldinger() {
        String aktorId = AktorIdProvider.getNext();
        DialogData dialogId = opprettNyDialog(aktorId);
        HenvendelseData henvendelseData = getHenvendelseData(dialogId, new Date());
        dialogDAO.opprettHenvendelse(henvendelseData);

        varselDAO.oppdaterSisteVarselForBruker(aktorId);

        val aktor = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0)
                .stream()
                .filter( aktør -> Objects.equals(aktør, aktorId))
                .toList();
        assertThat(aktor.size(), equalTo(0));
    }

    @Test
    void hentAktorerMedUlesteMeldingerEtterSisteVarsel_returnererIkkeDeUtenforGraceperiode() throws Exception {
        String aktorId = AktorIdProvider.getNext();
        DialogData dialogId = opprettNyDialog(aktorId);
        dialogDAO.opprettHenvendelse(getHenvendelseData(dialogId, getNowMinusSeconds(10)));

        val aktor1 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0)
                .stream()
                .filter(aktør -> Objects.equals(aktør, aktorId))
                .toList(); // Utenfor grace
        assertThat(aktor1.size(), equalTo(1));

        val aktor2 = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(TI_MINUTTER); // Innenfor grace
        assertThat(aktor2.size(), equalTo(0));
    }

    @Test
    void skalHenteBrukereMedUlesteMeldinger() {
        DialogData dialogId1 = opprettNyDialog("1111");
        HenvendelseData henvendelseData1 = getHenvendelseData(dialogId1, getNowMinusSeconds(30));
        dialogDAO.opprettHenvendelse(henvendelseData1);

        DialogData dialogId2 = opprettNyDialog("2222");
        HenvendelseData henvendelseData2 = getHenvendelseData(dialogId2, getNowMinusSeconds(20));
        dialogDAO.opprettHenvendelse(henvendelseData2);


        List<String> aktorer = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0);

        assertThat("skal inneholde alle",  aktorer.containsAll(List.of("1111", "2222")));
    }

    @Test
    void skalIkkeSendeVarselForHenvendelserSomerLagtInnAvBrukerenSelv() throws Exception {
        val before = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0); // Utenfor grace
        String aktorId = AktorIdProvider.getNext();
        DialogData dialogId = opprettNyDialog(aktorId);
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId.getId(), aktorId, AvsenderType.BRUKER).withSendt(getNowMinusSeconds(30)));

        val after = varselDAO.hentAktorerMedUlesteMeldingerEtterSisteVarsel(0); // Utenfor grace
        after.removeAll(before);
        assertThat(after.size(), equalTo(0));
    }

    private HenvendelseData getHenvendelseData(DialogData dialogData, Date sendt) {
        String aktorId = AktorIdProvider.getNext();
        HenvendelseData henvendelseData = nyHenvendelse(dialogData.getId(), aktorId, AvsenderType.VEILEDER)
                .withSendt(sendt);
        return henvendelseData;
    }

    private Date getNowMinusSeconds(int seconds) {
        return Date.from(Instant.now().minusSeconds(seconds));
    }

}
