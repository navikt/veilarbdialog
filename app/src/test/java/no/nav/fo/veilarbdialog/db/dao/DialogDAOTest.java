package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.*;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.VEILEDER;
import static no.nav.fo.veilarbdialog.domain.DialogStatus.builder;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogDAOTest extends IntegrasjonsTest {
    private static final String AKTOR_ID_1234 = "1234";

    @Inject
    private DialogDAO dialogDAO;

    @Test
    public void opprettDialog() {
        DialogData dialogData = nyDialog(AKTOR_ID_1234);
        dialogDAO.opprettDialog(dialogData);
        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(AKTOR_ID_1234);
        assertThat(dialoger).hasSize(1);
        DialogData hentetDialogData = dialoger.get(0);
        assertThat(hentetDialogData.getSisteStatusEndring()).isNotNull();
        assertThat(hentetDialogData).isEqualTo(dialogData
                .withId(hentetDialogData.getId())
                .withSisteStatusEndring(hentetDialogData.getSisteStatusEndring())
                .withOpprettetDato(hentetDialogData.getOpprettetDato())
        );
        assertThat(hentetDialogData.erFerdigbehandlet()).isTrue();
    }

    @Test
    public void hentDialogerForAktorId() {
        assertThat(dialogDAO.hentDialogerForAktorId(AKTOR_ID_1234)).isEmpty();
    }

    @Test
    public void hentDialog() {
        DialogData dialogData = nyDialog(AKTOR_ID_1234);
        long dialogId = dialogDAO.opprettDialog(dialogData);

        DialogData hentetDialog = dialogDAO.hentDialog(dialogId);

        assertThat(hentetDialog).isEqualTo(dialogData
                .withId(dialogId)
                .withSisteStatusEndring(hentetDialog.getSisteStatusEndring())
                .withOpprettetDato(hentetDialog.getOpprettetDato())
        );
        assertThat(hentetDialog.erFerdigbehandlet()).isTrue();
    }

    @Test
    public void opprettHenvendelse() {
        long dialogId = opprettNyDialog(AKTOR_ID_1234);

        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID_1234, AvsenderType.values()[0]);
        dialogDAO.opprettHenvendelse(henvendelseData);
        DialogData dialogMedHenvendelse = dialogDAO.hentDialogerForAktorId(AKTOR_ID_1234).get(0);
        HenvendelseData henvendelseUtenOpprettelsesDato = dialogMedHenvendelse.getHenvendelser().get(0).withSendt(null);
        assertThat(henvendelseUtenOpprettelsesDato).isEqualTo(henvendelseData.withId(henvendelseUtenOpprettelsesDato.getId()));

        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID_1234, AvsenderType.values()[0]));
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID_1234, AvsenderType.values()[0]));

        DialogData dialogMedHenvendelser = dialogDAO.hentDialogerForAktorId(AKTOR_ID_1234).get(0);
        assertThat(dialogMedHenvendelser.getHenvendelser()).hasSize(3);
    }

    @Test
    public void markerDialogSomLest() {
        long dialogId = opprettNyDialog(AKTOR_ID_1234);

        dialogDAO.markerDialogSomLestAvBruker(dialogId);
        dialogDAO.markerDialogSomLestAvVeileder(dialogId);

        DialogData dialog = dialogDAO.hentDialog(dialogId);

        assertThat(dialog.erLestAvBruker()).isTrue();
        assertThat(dialog.erLestAvVeileder()).isTrue();
    }

    @Test
    public void hentDialogForAktivitetId() {
        String aktivitetId = "aktivitetId";
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isEmpty();
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().aktivitetId(aktivitetId).build());
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isPresent();
    }

    @Test
    public void oppdaterDialogStatus_oppdatererStatusFelter() {
        long dialogId = opprettNyDialog(AKTOR_ID_1234);
        DialogData dialogData = dialogDAO.hentDialog(dialogId);

        Date tidspunktForOppdatering = uniktTidspunkt();
        dialogDAO.oppdaterVentePaSvarTidspunkt(builder()
                .dialogId(dialogId)
                .venterPaSvar(true)
                .build()
        );

        DialogData oppdatertDialog = dialogDAO.hentDialog(dialogId);
        assertThat(oppdatertDialog.getSisteStatusEndring()).isAfter(tidspunktForOppdatering);
        assertThat(oppdatertDialog.getVenterPaSvarTidspunkt()).isAfter(tidspunktForOppdatering);
        assertThat(oppdatertDialog.venterPaSvar()).isTrue();
        assertThat(oppdatertDialog).isEqualTo(dialogData
                .withSisteStatusEndring(oppdatertDialog.getSisteStatusEndring())
                .withVenterPaSvarTidspunkt(oppdatertDialog.getVenterPaSvarTidspunkt())
        );
    }


    @Test
    public void oppdaterFerdigbehandletTidspunkt_oppdatererStatusFelter() {
        long dialogId = opprettNyDialog(AKTOR_ID_1234);
        DialogData dialogData = dialogDAO.hentDialog(dialogId);

        Date tidspunktForOppdatering = uniktTidspunkt();
        dialogDAO.oppdaterFerdigbehandletTidspunkt(builder()
                .dialogId(dialogId)
                .venterPaSvar(true)
                .ferdigbehandlet(true)
                .build()
        );

        DialogData oppdatertDialog = dialogDAO.hentDialog(dialogId);
        assertThat(oppdatertDialog.getSisteStatusEndring()).isAfter(tidspunktForOppdatering);
        assertThat(oppdatertDialog.getFerdigbehandletTidspunkt()).isAfter(tidspunktForOppdatering);
        assertThat(oppdatertDialog.getUbehandletTidspunkt()).isNull();
        assertThat(oppdatertDialog.erFerdigbehandlet()).isTrue();
        assertThat(oppdatertDialog).isEqualTo(dialogData
                .withSisteStatusEndring(oppdatertDialog.getSisteStatusEndring())
                .withFerdigbehandletTidspunkt(oppdatertDialog.getFerdigbehandletTidspunkt())
                .withUbehandletTidspunkt(oppdatertDialog.getUbehandletTidspunkt())
        );
    }

    @Test
    public void oppdaterDialogStatus_statusTilbakestillesVedNyBrukerHenvendelse() {
        long dialogId = opprettNyDialog(AKTOR_ID_1234);
        dialogDAO.oppdaterVentePaSvarTidspunkt(builder()
                .dialogId(dialogId)
                .venterPaSvar(true)
                .ferdigbehandlet(true)
                .build()
        );
        DialogData dialogForOppdatering = dialogDAO.hentDialog(dialogId);
        assertThat(dialogForOppdatering.venterPaSvar()).isTrue();
        assertThat(dialogForOppdatering.erFerdigbehandlet()).isTrue();

        uniktTidspunkt();
        HenvendelseData veilederHenvendelseData = nyHenvendelse(dialogId, AKTOR_ID_1234, VEILEDER);
        dialogDAO.opprettHenvendelse(veilederHenvendelseData);

        DialogData dialogMedVeilederHenvendelse = dialogDAO.hentDialog(dialogId);
        assertThat(dialogMedVeilederHenvendelse.venterPaSvar()).isTrue();
        assertThat(dialogMedVeilederHenvendelse.erFerdigbehandlet()).isTrue();

        uniktTidspunkt();
        HenvendelseData dialogMedBrukerHenvendelse = nyHenvendelse(dialogId, AKTOR_ID_1234, BRUKER);
        dialogDAO.opprettHenvendelse(dialogMedBrukerHenvendelse);

        DialogData oppdatertDialog = dialogDAO.hentDialog(dialogId);
        assertThat(oppdatertDialog.venterPaSvar()).isFalse();
        assertThat(oppdatertDialog.erFerdigbehandlet()).isFalse();
    }

    @Test
    public void hentAktorerMedEndringerEtter_nyDialog_aktorEndret() {
        Date ettSekundSiden = new Date(System.currentTimeMillis() - 1000L);
        Date omEttSekund = new Date(System.currentTimeMillis() + 1000L);

        assertThat(dialogDAO.hentAktorerMedEndringerFOM(ettSekundSiden, 500)).isEmpty();
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(omEttSekund, 500)).isEmpty();

        opprettNyDialog(AKTOR_ID_1234);
        opprettNyDialog("5678");

        assertThat(dialogDAO.hentAktorerMedEndringerFOM(ettSekundSiden, 500)).hasSize(2);
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(omEttSekund, 500)).isEmpty();
    }

    @Test
    public void hentAktorerMedEndringerEtter_statusPaaFeedskalTaHensynTilAlleAktorensDialoger() throws InterruptedException {
        Date ettSekundSiden = new Date(System.currentTimeMillis() - 1000L);

        DialogData nyDialog = nyDialog(AKTOR_ID_1234);
        long dialogId = dialogDAO.opprettDialog(nyDialog);
        dialogDAO.oppdaterVentePaSvarTidspunkt(new DialogStatus(dialogId, true, false));
        dialogDAO.oppdaterFerdigbehandletTidspunkt(new DialogStatus(dialogId, false, false));

        List<DialogAktor> endringerForEttSekundSiden = dialogDAO.hentAktorerMedEndringerFOM(ettSekundSiden, 500);
        assertThat(endringerForEttSekundSiden).hasSize(1);
        Date tidspunktEldsteVentende = endringerForEttSekundSiden.get(0).getTidspunktEldsteVentende();
        Date ubehandletTidspunkt = endringerForEttSekundSiden.get(0).getTidspunktEldsteUbehandlede();
        assertThat(tidspunktEldsteVentende).isNotNull();
        assertThat(ubehandletTidspunkt).isNotNull();

        Thread.sleep(1);
        Date forrigeLeseTidspunkt = new Date();
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234));

        List<DialogAktor> endringerEtterForrigeLesetidspunkt = dialogDAO.hentAktorerMedEndringerFOM(forrigeLeseTidspunkt, 500);
        assertThat(endringerEtterForrigeLesetidspunkt).hasSize(1);
        Date nyttTidspunktEldsteVentende = endringerEtterForrigeLesetidspunkt.get(0).getTidspunktEldsteVentende();
        Date nyttUbehandletTidspunkt = endringerEtterForrigeLesetidspunkt.get(0).getTidspunktEldsteUbehandlede();
        assertThat(nyttTidspunktEldsteVentende).isEqualTo(tidspunktEldsteVentende);
        assertThat(nyttUbehandletTidspunkt).isEqualTo(nyttUbehandletTidspunkt);

    }

    @Test
    public void hentAktorerMedEndringerFOM_oppdaterDialogStatusOgNyHenvendelse_riktigStatus() {
        long dialogId = opprettNyDialog(AKTOR_ID_1234);

        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID_1234, AvsenderType.values()[0]);
        dialogDAO.opprettHenvendelse(henvendelseData);

        Date forForsteStatusOppdatering = uniktTidspunkt();
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(forForsteStatusOppdatering, 500)).isEmpty();

        DialogStatus.DialogStatusBuilder dialogStatusBuilder = builder().dialogId(dialogId);

        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatusBuilder
                .venterPaSvar(true)
                .build()
        );

        Date etterForsteStatusOppdatering = uniktTidspunkt();
        DialogAktor etterForsteOppdatering = hentAktorMedEndringerEtter(forForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.sisteEndring).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteVentende).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteUbehandlede).isBefore(forForsteStatusOppdatering);

        Date forAndreStatusOppdatering = uniktTidspunkt();
        assertThat(dialogDAO.hentAktorerMedEndringerFOM(forAndreStatusOppdatering, 500)).isEmpty();
        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatusBuilder
                .ferdigbehandlet(true)
                .build()
        );
        uniktTidspunkt();

        DialogAktor etterAndreOppdatering = hentAktorMedEndringerEtter(forAndreStatusOppdatering);
        assertThat(etterAndreOppdatering.sisteEndring).isBetween(forAndreStatusOppdatering, uniktTidspunkt());
        assertThat(etterAndreOppdatering.tidspunktEldsteVentende).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterAndreOppdatering.tidspunktEldsteUbehandlede).isNull();

        Date forNyHenvendelse = uniktTidspunkt();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID_1234, AvsenderType.values()[0]));

        DialogAktor etterNyHenvenselse = hentAktorMedEndringerEtter(forNyHenvendelse);
        assertThat(etterNyHenvenselse.sisteEndring).isBetween(forNyHenvendelse, uniktTidspunkt());
        assertThat(etterNyHenvenselse.tidspunktEldsteVentende).isNull();
        assertThat(etterNyHenvenselse.tidspunktEldsteUbehandlede).isBetween(forNyHenvendelse, uniktTidspunkt());
    }

    @Test
    public void hentGjeldendeDialogerForAktorId() throws Exception {
        val dialog = nyDialog(AKTOR_ID_1234).toBuilder().overskrift("ny").build();
        val historiskDialog = nyDialog(AKTOR_ID_1234).toBuilder().historisk(true).overskrift("historisk").build();

        dialogDAO.opprettDialog(dialog);
        dialogDAO.opprettDialog(historiskDialog);

        dialogDAO.settDialogTilHistoriskOgOppdaterFeed(historiskDialog);

        val gjeldendeDialoger = dialogDAO.hentGjeldendeDialogerForAktorId(AKTOR_ID_1234);
        assertThat(gjeldendeDialoger).hasSize(1);
        assertThat(gjeldendeDialoger.get(0).getOverskrift()).isEqualTo("ny");
    }

    @Test
    public void settDialogTilHistoriskOgOppdaterFeed_dialog_blir_historisk() {
        long dialogId = dialogDAO.opprettDialog(nyDialog());
        DialogData dialog = dialogDAO.hentDialog(dialogId);
        assertThat(dialog.isHistorisk()).isFalse();

        dialogDAO.settDialogTilHistoriskOgOppdaterFeed(dialog);

        assertThat(dialogDAO.hentDialog(dialogId).isHistorisk()).isTrue();
    }

    private DialogAktor hentAktorMedEndringerEtter(Date tidspunkt) {
        List<DialogAktor> endredeAktorer = dialogDAO.hentAktorerMedEndringerFOM(tidspunkt, 500);
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

    private long opprettNyDialog(String aktorId) {
        return dialogDAO.opprettDialog(nyDialog(aktorId));
    }
}