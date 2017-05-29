package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static no.nav.fo.veilarbdialog.domain.DialogStatus.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DialogDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";

    @Inject
    private DialogDAO dialogDAO;

    @Before
    public void setup(){
        dialogDAO.dateProvider = mock(DateProvider.class);
        when(dialogDAO.dateProvider.getNow()).thenAnswer(DialogDAOTest::timestampFromSystemTime);
    }

    @Test
    public void opprettDialog() {
        DialogData dialogData = nyDialog(AKTOR_ID);
        dialogDAO.opprettDialog(dialogData);
        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(AKTOR_ID);
        assertThat(dialoger).hasSize(1);
        DialogData hentetDialogData = dialoger.get(0);
        assertThat(hentetDialogData.sisteStatusEndring).isNotNull();
        assertThat(hentetDialogData).isEqualTo(dialogData.toBuilder()
                .id(hentetDialogData.id)
                .sisteStatusEndring(hentetDialogData.sisteStatusEndring)
                .build()
        );
    }

    @Test
    public void hentDialogerForAktorId() {
        assertThat(dialogDAO.hentDialogerForAktorId(AKTOR_ID)).isEmpty();
    }

    @Test
    public void hentDialog() {
        DialogData dialogData = nyDialog(AKTOR_ID);
        long dialogId = dialogDAO.opprettDialog(dialogData);

        DialogData hentetDialog = dialogDAO.hentDialog(dialogId);

        assertThat(hentetDialog).isEqualTo(dialogData.toBuilder()
                .id(dialogId)
                .sisteStatusEndring(hentetDialog.sisteStatusEndring)
                .build()
        );
    }

    @Test
    public void opprettHenvendelse() {
        long dialogId = opprettNyDialog();

        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID);
        dialogDAO.opprettHenvendelse(henvendelseData);
        DialogData dialogMedHenvendelse = dialogDAO.hentDialogerForAktorId(AKTOR_ID).get(0);
        HenvendelseData henvendelseUtenOpprettelsesDato = dialogMedHenvendelse.getHenvendelser().get(0).toBuilder().sendt(null).build();
        assertThat(henvendelseUtenOpprettelsesDato).isEqualTo(henvendelseData);

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));

        DialogData dialogMedHenvendelser = dialogDAO.hentDialogerForAktorId(AKTOR_ID).get(0);
        assertThat(dialogMedHenvendelser.henvendelser).hasSize(3);
    }

    @Test
    public void markerDialogSomLest() {
        long dialogId = opprettNyDialog();

        dialogDAO.markerDialogSomLestAvBruker(dialogId);
        dialogDAO.markerDialogSomLestAvVeileder(dialogId);

        DialogData dialog = dialogDAO.hentDialog(dialogId);

        assertThat(dialog.lestAvBruker).isTrue();
        assertThat(dialog.lestAvVeileder).isTrue();
    }

    @Test
    public void hentDialogForAktivitetId() {
        String aktivitetId = "aktivitetId";
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isEmpty();
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID).toBuilder().aktivitetId(aktivitetId).build());
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isPresent();
    }

    @Test
    public void oppdaterDialogStatus_oppdatererStatusFelter() {
        long dialogId = opprettNyDialog();
        DialogData dialogData = dialogDAO.hentDialog(dialogId);

        Date tidspunktForOppdatering = uniktTidspunkt();
        dialogDAO.oppdaterDialogStatus(builder()
                .dialogId(dialogId)
                .venterPaSvar(true)
                .ferdigbehandlet(true)
                .build()
        );

        DialogData oppdatertDialog = dialogDAO.hentDialog(dialogId);
        assertThat(oppdatertDialog.sisteStatusEndring).isAfter(tidspunktForOppdatering);
        assertThat(oppdatertDialog).isEqualTo(dialogData.toBuilder()
                .venterPaSvar(true)
                .ferdigbehandlet(true)
                .sisteStatusEndring(oppdatertDialog.sisteStatusEndring)
                .build()
        );
    }

    @Test
    public void hentAktorerMedEndringerEtter_nyDialog_aktorEndret() {
        Date ettSekundSiden = new Date(System.currentTimeMillis() - 1000L);
        Date omEttSekund = new Date(System.currentTimeMillis() + 1000L);

        assertThat(dialogDAO.hentAktorerMedEndringerEtter(ettSekundSiden)).isEmpty();
        assertThat(dialogDAO.hentAktorerMedEndringerEtter(omEttSekund)).isEmpty();

        opprettNyDialog();

        assertThat(dialogDAO.hentAktorerMedEndringerEtter(ettSekundSiden)).hasSize(1);
        assertThat(dialogDAO.hentAktorerMedEndringerEtter(omEttSekund)).isEmpty();
    }

    @Test
    public void hentAktorerMedEndringerEtter_oppdaterDialogStatusOgNyHenvendelse_riktigStatus() {
        long dialogId = opprettNyDialog();

        DialogStatus.DialogStatusBuilder dialogStatusBuilder = builder()
                .dialogId(dialogId)
                .venterPaSvar(true);

        Date forForsteStatusOppdatering = uniktTidspunkt();
        assertThat(dialogDAO.hentAktorerMedEndringerEtter(forForsteStatusOppdatering)).isEmpty();
        dialogDAO.oppdaterDialogStatus(dialogStatusBuilder.build());

        DialogAktor etterForsteOppdatering = hentAktorMedEndringerEtter(forForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.sisteEndring).isBetween(forForsteStatusOppdatering, uniktTidspunkt());
        assertThat(etterForsteOppdatering.venterPaSvar).isTrue();
        assertThat(etterForsteOppdatering.ubehandlet).isTrue();

        Date forAndreStatusOppdatering = uniktTidspunkt();
        assertThat(dialogDAO.hentAktorerMedEndringerEtter(forAndreStatusOppdatering)).isEmpty();
        dialogDAO.oppdaterDialogStatus(dialogStatusBuilder
                .ferdigbehandlet(true)
                .build()
        );

        DialogAktor etterAndreOppdatering = hentAktorMedEndringerEtter(forAndreStatusOppdatering);
        assertThat(etterAndreOppdatering.sisteEndring).isBetween(forAndreStatusOppdatering, uniktTidspunkt());
        assertThat(etterAndreOppdatering.venterPaSvar).isTrue();
        assertThat(etterAndreOppdatering.ubehandlet).isFalse();

        Date forNyHenvendelse = uniktTidspunkt();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));

        DialogAktor etterNyHenvenselse = hentAktorMedEndringerEtter(forNyHenvendelse);
        assertThat(etterNyHenvenselse.sisteEndring).isBetween(forNyHenvendelse, uniktTidspunkt());
        assertThat(etterNyHenvenselse.venterPaSvar).isFalse();
        assertThat(etterNyHenvenselse.ubehandlet).isTrue();
    }

    private DialogAktor hentAktorMedEndringerEtter(Date tidspunkt) {
        List<DialogAktor> endredeAktorer = dialogDAO.hentAktorerMedEndringerEtter(tidspunkt);
        assertThat(endredeAktorer).hasSize(1);
        return endredeAktorer.get(0);
    }

    @SneakyThrows
    private Date uniktTidspunkt() {
        sleep(1);
        Date tidspunkt = new Date();
        sleep(1);
        return tidspunkt;
    }

    private long opprettNyDialog() {
        return dialogDAO.opprettDialog(nyDialog(AKTOR_ID));
    }

    @SuppressWarnings("unused")
    static String timestampFromSystemTime(InvocationOnMock invocationOnMock) {
        return String.format("\'%s\'", new Timestamp(System.currentTimeMillis()));
    }

}