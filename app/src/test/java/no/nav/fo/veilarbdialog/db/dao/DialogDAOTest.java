package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.domain.Status;
import no.nav.fo.veilarbdialog.service.MetadataService;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogDAOTest extends IntegrasjonsTest {
    private static final String AKTOR_ID_1234 = "1234";

    @Inject
    private DialogDAO dialogDAO;

    @Test
    public void kan_opprette_dialog() {
        DialogData dialog = nyDialog();
        Date uniktTidspunkt = uniktTidspunkt();
        DialogData dialogData = dialogDAO.opprettDialog(dialog);
        assertThat(dialogData.getOppdatert()).isAfter(uniktTidspunkt);
        assertThat(dialogData.getOpprettetDato()).isAfter(uniktTidspunkt);

        assertThat(dialogData).isEqualTo(dialog
                .withId(dialogData.getId())
                .withOppdatert(dialogData.getOppdatert())
                .withOpprettetDato(dialogData.getOpprettetDato())
        );
    }

    @Test
    public void kanHenteDialogerPaaAktorId() {
        DialogData dialogData = nyDialog(AKTOR_ID_1234);
        dialogDAO.opprettDialog(dialogData);

        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(AKTOR_ID_1234);
        assertThat(dialoger).hasSize(1);

        DialogData hentetDialogData = dialoger.get(0);
        assertThat(hentetDialogData.getOppdatert()).isNotNull();

        assertThat(hentetDialogData).isEqualTo(dialogData
                .withId(hentetDialogData.getId())
                .withOppdatert(hentetDialogData.getOppdatert())
                .withOpprettetDato(hentetDialogData.getOpprettetDato())
        );
        assertThat(hentetDialogData.erFerdigbehandlet()).isTrue();
    }

    @Test
    public void kanHentDialogPaDialogId() {
        DialogData dialogData = nyDialog(AKTOR_ID_1234);
        DialogData opprettDialog = dialogDAO.opprettDialog(dialogData);

        DialogData hentetDialog = dialogDAO.hentDialog(opprettDialog.getId());

        assertThat(hentetDialog).isEqualTo(dialogData
                .withId(opprettDialog.getId())
                .withOppdatert(hentetDialog.getOppdatert())
                .withOpprettetDato(hentetDialog.getOpprettetDato())
        );
        assertThat(hentetDialog.erFerdigbehandlet()).isTrue();
    }

    @Test
    public void kanOppretteHenvendelse() {
        DialogData dialogData = opprettNyDialog(AKTOR_ID_1234);
        HenvendelseData henvendelseData = nyHenvendelse(dialogData.getId(), AKTOR_ID_1234, AvsenderType.BRUKER);

        long henvendelseId = dialogDAO.opprettHenvendelse(henvendelseData);
        List<HenvendelseData> henvendelser = dialogDAO.hentDialog(dialogData.getId()).getHenvendelser();

        assertThat(henvendelser.size()).isEqualTo(1);
        HenvendelseData opprettet = henvendelser.get(0);
        assertThat(opprettet).isEqualTo(henvendelseData.withId(henvendelseId));
    }


    @Test
    public void kanHenteDialogPaaAktivitetId() {
        String aktivitetId = "aktivitetId";
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isEmpty();
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().aktivitetId(aktivitetId).build());
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isPresent();
    }

    @Test
    public void hentDialogerSomSkalAvsluttesForAktorIdTarIkkeMedAlleredeHistoriske() {
        DialogData dialog = nyDialog(AKTOR_ID_1234)
                .toBuilder()
                .overskrift("ny")
                .build();

        DialogData historiskDialog = nyDialog(AKTOR_ID_1234)
                .toBuilder()
                .historisk(true)
                .overskrift("historisk")
                .build();

        dialogDAO.opprettDialog(dialog);
        dialogDAO.opprettDialog(historiskDialog);

        List<DialogData> dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, new Date(System.currentTimeMillis() + 1000));
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("ny");
    }

    @Test
    public void hentDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerNyereEnnUtmeldingstidspunkt() {
        DialogData dialog = nyDialog(AKTOR_ID_1234).toBuilder().overskrift("gammel").build();
        dialogDAO.opprettDialog(dialog);

        Date avslutningsdato = uniktTidspunkt();

        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("ny").build());

        List<DialogData> dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");
    }

    @Test
    public void skalOppdatereStatus() {
        DialogData dialog = dialogDAO.opprettDialog(nyDialog());

        Status status = new Status(dialog.getId());
        status.setHistorisk(true);
        status.settVenterPaSvarFraBruker(uniktTidspunkt());
        status.settVenterPaNavSiden(uniktTidspunkt());
        status.setUlesteMeldingerForVeileder(uniktTidspunkt());
        status.setUlesteMeldingerForBruker(uniktTidspunkt());

        Date uniktTidspunkt = uniktTidspunkt();
        dialogDAO.oppdaterStatus(status);

        DialogData dialogData = dialogDAO.hentDialog(dialog.getId());
        Status oppdatert = MetadataService.getStatus(dialogData);

        assertThat(oppdatert).isEqualTo(status);
        assertThat(dialogData.getOppdatert()).isAfter(uniktTidspunkt);
    }

    @Test
    public void skalIkkeLoggeTekstInnhold() {
        String sensitivtInnhold = "Sensitivt innhold";

        List<HenvendelseData> henvendelser = asList(
                HenvendelseData.builder().id(1L).tekst(sensitivtInnhold).build(),
                HenvendelseData.builder().id(2L).tekst(sensitivtInnhold).build()
        );
        DialogData dialog = DialogData.builder()
                .id(1L)
                .henvendelser(henvendelser)
                .build();


        String henvendelseStr = henvendelser.get(0).toString();
        String dialogStr = dialog.toString();

        assertThat(henvendelseStr).doesNotContain(sensitivtInnhold);
        assertThat(dialogStr).doesNotContain(sensitivtInnhold);
    }

    private DialogData opprettNyDialog(String aktorId) {
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