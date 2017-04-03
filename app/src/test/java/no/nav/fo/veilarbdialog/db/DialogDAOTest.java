package no.nav.fo.veilarbdialog.db;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.hamcrest.CoreMatchers.equalTo;
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
        assertThat(dialogDAO.hentDialogerForAktorId(AKTOR_ID), hasSize(1));
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

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogData.id));
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogData.id));
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogData.id));

        DialogData dialogMedHenvendelser = dialogDAO.hentDialogerForAktorId(AKTOR_ID).get(0);
        assertThat(dialogMedHenvendelser.henvendelser, hasSize(3));
    }

    private DialogData opprettNyDialog() {
        return dialogDAO.opprettDialog(nyDialog(AKTOR_ID));
    }

}