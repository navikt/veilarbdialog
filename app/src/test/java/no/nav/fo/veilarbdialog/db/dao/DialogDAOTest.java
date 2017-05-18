package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class DialogDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";

    @Inject
    private DialogDAO dialogDAO;

    @Test
    public void opprettDialog() {
        opprettNyDialog();
        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(AKTOR_ID);
        assertThat(dialoger, hasSize(1));
        DialogData dialogData = dialoger.get(0);
        assertThat(dialogData.lestAvBruker, notNullValue());
        assertThat(dialogData.lestAvVeileder, notNullValue());
    }

    @Test
    public void hentDialogerForAktorId() {
        assertThat(dialogDAO.hentDialogerForAktorId(AKTOR_ID), hasSize(0));
    }


    @Test
    public void hentDialog() {
        DialogData dialogData = opprettNyDialog();
        assertThat(dialogDAO.hentDialog(dialogData.id), equalTo(dialogData));
    }

    @Test
    public void opprettHenvendelse() {
        DialogData dialogData = opprettNyDialog();

        HenvendelseData henvendelseData = nyHenvendelse(dialogData);
        dialogDAO.opprettHenvendelse(henvendelseData);
        DialogData dialogMedHenvendelse = dialogDAO.hentDialogerForAktorId(AKTOR_ID).get(0);
        HenvendelseData henvendelseUtenOpprettelsesDato = dialogMedHenvendelse.getHenvendelser().get(0).toBuilder().sendt(null).build();
        assertThat(henvendelseUtenOpprettelsesDato, equalTo(henvendelseData));

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogData));
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogData));

        DialogData dialogMedHenvendelser = dialogDAO.hentDialogerForAktorId(AKTOR_ID).get(0);
        assertThat(dialogMedHenvendelser.henvendelser, hasSize(3));
    }

    @Test
    public void markerDialogSomLest() {
        DialogData dialogData = opprettNyDialog();

        dialogDAO.markerDialogSomLestAvBruker(dialogData.id);
        dialogDAO.markerDialogSomLestAvVeileder(dialogData.id);

        DialogData dialog = dialogDAO.hentDialog(dialogData.id);

        Date snart = new Date(System.currentTimeMillis() + 1);
        assertThat(dialog.lestAvBruker.before(snart), is(true));
        assertThat(dialog.lestAvVeileder.before(snart), is(true));
    }

    @Test
    public void hentDialogForAktivitetId() {
        String aktivitetId = "aktivitetId";
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId).isPresent(), is(false));
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID).toBuilder().aktivitetId(aktivitetId).build());
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId).isPresent(), is(true));
    }

    private DialogData opprettNyDialog() {
        return dialogDAO.opprettDialog(nyDialog(AKTOR_ID));
    }

}