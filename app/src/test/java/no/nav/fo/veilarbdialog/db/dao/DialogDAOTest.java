package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import lombok.val;
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
                .ferdigbehandlet(true)
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
                .ferdigbehandlet(true)
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
    public void oppdaterDialogStatus_statusTilbakestillesVedNyHenvendelse() {
        long dialogId = opprettNyDialog();
        dialogDAO.oppdaterDialogStatus(builder()
                .dialogId(dialogId)
                .venterPaSvar(true)
                .ferdigbehandlet(true)
                .build()
        );

        uniktTidspunkt();

        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID);
        dialogDAO.opprettHenvendelse(henvendelseData);

        DialogData oppdatertDialog = dialogDAO.hentDialog(dialogId);
        assertThat(oppdatertDialog.venterPaSvar).isFalse();
        assertThat(oppdatertDialog.ferdigbehandlet).isFalse();
    }


    @Test
    public void hentAktorerMedEndringerEtter_nyDialog_aktorEndret() {
        Date ettSekundSiden = new Date(System.currentTimeMillis() - 1000L);
        Date omEttSekund = new Date(System.currentTimeMillis() + 1000L);

        assertThat(dialogDAO.hentAktorerMedEndringerFOM(ettSekundSiden)).isEmpty();
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(omEttSekund)).isEmpty();

        opprettNyDialog();

        assertThat(dialogDAO.hentAktorerMedEndringerFOM(ettSekundSiden)).hasSize(1);
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(omEttSekund)).isEmpty();
    }

    @Test
    public void hentAktorerMedEndringerFOM_oppdaterDialogStatusOgNyHenvendelse_riktigStatus() {
        long dialogId = opprettNyDialog();

        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID);
        dialogDAO.opprettHenvendelse(henvendelseData);

        Date forForsteStatusOppdatering = uniktTidspunkt();
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(forForsteStatusOppdatering)).isEmpty();

        DialogStatus.DialogStatusBuilder dialogStatusBuilder = builder()
                .dialogId(dialogId)
                .venterPaSvar(true);
        dialogDAO.oppdaterDialogStatus(dialogStatusBuilder.build());

        Date etterForsteStatusOppdatering = uniktTidspunkt();
        DialogAktor etterForsteOppdatering = hentAktorMedEndringerEtter(forForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.sisteEndring).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteVentende).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteUbehandlede).isBefore(forForsteStatusOppdatering);

        Date forAndreStatusOppdatering = uniktTidspunkt();
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(forAndreStatusOppdatering)).isEmpty();
        dialogDAO.oppdaterDialogStatus(dialogStatusBuilder
                .ferdigbehandlet(true)
                .venterPaSvar(false)
                .build()
        );
        uniktTidspunkt();

        DialogAktor etterAndreOppdatering = hentAktorMedEndringerEtter(forAndreStatusOppdatering);
        assertThat(etterAndreOppdatering.sisteEndring).isBetween(forAndreStatusOppdatering, uniktTidspunkt());
        assertThat(etterAndreOppdatering.tidspunktEldsteVentende).isNull();
        assertThat(etterAndreOppdatering.tidspunktEldsteUbehandlede).isNull();

        Date forNyHenvendelse = uniktTidspunkt();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID));

        DialogAktor etterNyHenvenselse = hentAktorMedEndringerEtter(forNyHenvendelse);
        assertThat(etterNyHenvenselse.sisteEndring).isBetween(forNyHenvendelse, uniktTidspunkt());
        assertThat(etterNyHenvenselse.tidspunktEldsteVentende).isNull();
        assertThat(etterNyHenvenselse.tidspunktEldsteUbehandlede).isBetween(forNyHenvendelse, uniktTidspunkt());
    }

    @Test
    public void hentGjeldendeDialogerForAktorId() throws Exception {
        val dialog = nyDialog(AKTOR_ID).toBuilder().overskrift("ny").build();
        val historiskDialog = nyDialog(AKTOR_ID).toBuilder().historisk(true).overskrift("historisk").build();

        dialogDAO.opprettDialog(dialog);
        dialogDAO.opprettDialog(historiskDialog);

        dialogDAO.settDialogTilHistoriskOgOppdaterFeed(historiskDialog);

        val gjeldendeDialoger = dialogDAO.hentGjeldendeDialogerForAktorId(AKTOR_ID);
        assertThat(gjeldendeDialoger).hasSize(1);
        assertThat(gjeldendeDialoger.get(0).overskrift).isEqualTo("ny");
    }

    private DialogAktor hentAktorMedEndringerEtter(Date tidspunkt) {
        List<DialogAktor> endredeAktorer = dialogDAO.hentAktorerMedEndringerFOM(tidspunkt);
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