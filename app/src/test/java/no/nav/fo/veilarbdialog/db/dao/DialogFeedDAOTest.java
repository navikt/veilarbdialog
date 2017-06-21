package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.junit.Test;

import javax.inject.Inject;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogFeedDAOTest extends IntegrasjonsTest {
    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private DialogFeedDAO dialogFeedDAO;

    @Test
    public void skal_returnere_null_hvis_ingen_historiske_i_databasen() {
        val dialog = dialogFeedDAO.hentSisteHistoriskeTidspunkt();
        assertThat(dialog).isNull();
    }

    @Test
    public void skal_ha_siste_dato_for_historiske_dialoger() {
        val endret1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = dateFromISO8601("2010-12-04T10:15:30+02:00");

        String aktorId = "123";
        val dialog1 = nyDialog(aktorId).toBuilder().historiskDato(endret1).build();
        val dialog2 = nyDialog(aktorId).toBuilder().historiskDato(endret2).build();

        dialogDAO.opprettDialog(dialog1);
        dialogDAO.opprettDialog(dialog2);

        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        assertThat(dialoger).hasSize(2);

        dialogDAO.settDialogTilHistorisk(dialog1);
        dialogDAO.settDialogTilHistorisk(dialog2);

        val sisteDialogTidspunkt = dialogFeedDAO.hentSisteHistoriskeTidspunkt();
        assertThat(sisteDialogTidspunkt).isEqualTo(endret2);
    }

    private static Date dateFromISO8601(String date) {
        Instant instant =  ZonedDateTime.parse(date).toInstant();
        return Date.from(instant);
    }
}
