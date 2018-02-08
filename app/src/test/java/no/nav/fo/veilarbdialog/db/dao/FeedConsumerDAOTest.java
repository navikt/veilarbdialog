package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.service.MetadataService;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static org.assertj.core.api.Assertions.assertThat;

public class FeedConsumerDAOTest extends IntegrasjonsTest {
    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private MetadataService metadataService;

    @Inject
    private FeedConsumerDAO feedConsumerDAO;

    @Test
    public void skal_ha_siste_dato_tidligere_enn_now‍() {
        String aktorId = "123";
        DialogData dialog1 = nyDialog(aktorId).toBuilder().historisk(true).build();
        DialogData dialog2 = nyDialog(aktorId).toBuilder().historisk(true).build();

        dialogDAO.opprettDialog(dialog1);
        dialogDAO.opprettDialog(dialog2);

        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        List<DialogData> gjeldendeDialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktorId, null);
        assertThat(dialoger).hasSize(2);
        assertThat(gjeldendeDialoger).isEmpty();

        Date tidspunktEtterHistorisk = new Date();

        Date sisteDialogTidspunkt = feedConsumerDAO.hentSisteHistoriskeTidspunkt();
        assertThat(sisteDialogTidspunkt).isBeforeOrEqualsTo(tidspunktEtterHistorisk);
    }
}
