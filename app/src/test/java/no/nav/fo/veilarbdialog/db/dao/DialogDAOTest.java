package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
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
        assertThat(oppdatertDialog.getOppdatert()).isAfter(tidspunktForOppdatering);
        assertThat(oppdatertDialog.venterPaSvar()).isTrue();
        assertThat(oppdatertDialog).isEqualTo(dialogData
                .withSisteStatusEndring(oppdatertDialog.getSisteStatusEndring())
                .withVenterPaSvarTidspunkt(oppdatertDialog.getVenterPaSvarTidspunkt())
                .withOppdatert(oppdatertDialog.getOppdatert())
                .withVenterPaSvarFraBruker(oppdatertDialog.getVenterPaSvarFraBruker())
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
    public void hentDialogerSomSkalAvsluttesForAktorIdTarIkkeMedAlleredeHistoriske() {
        val dialog = nyDialog(AKTOR_ID_1234).toBuilder().overskrift("ny").build();
        val historiskDialog = nyDialog(AKTOR_ID_1234).toBuilder().historisk(true).overskrift("historisk").build();

        dialogDAO.opprettDialog(dialog);
        dialogDAO.opprettDialog(historiskDialog);

        dialogDAO.oppdaterDialogTilHistorisk(historiskDialog);

        val dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, new Date(System.currentTimeMillis() + 1000));
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("ny");
    }

    @Test
    public void hentDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerNyereEnnUtmeldingstidspunkt() throws Exception {
        val dialog = nyDialog(AKTOR_ID_1234).toBuilder().overskrift("gammel").build();
        dialogDAO.opprettDialog(dialog);
        Thread.sleep(10);
        Date avslutningsdato = new Date();
        
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("ny").build());

        val dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");
    }

    @Test
    public void settDialogTilHistoriskOgOppdaterFeed_dialog_blir_historisk() {
        long dialogId = dialogDAO.opprettDialog(nyDialog());
        DialogData dialog = dialogDAO.hentDialog(dialogId);
        assertThat(dialog.isHistorisk()).isFalse();

        dialogDAO.oppdaterDialogTilHistorisk(dialog);

        assertThat(dialogDAO.hentDialog(dialogId).isHistorisk()).isTrue();
    }

    private long opprettNyDialog(String aktorId) {
        return dialogDAO.opprettDialog(nyDialog(aktorId));
    }

    @SneakyThrows
    private Date uniktTidspunkt() {
        sleep(1);
        Date tidspunkt = new Date();
        sleep(1);
        return tidspunkt;
    }

}