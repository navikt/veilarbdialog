package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.IntegationTest;
import no.nav.fo.veilarbdialog.domain.*;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogDAOTest extends IntegationTest {

    private static final String AKTOR_ID_1234 = "1234";

    @Inject
    private DialogDAO dialogDAO;

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(Arrays.asList(DialogDAO.class));
    }

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

        Date uniktTidspunkt = uniktTidspunkt();
        long henvendelseId = dialogDAO.opprettHenvendelse(henvendelseData).getId();
        List<HenvendelseData> henvendelser = dialogDAO.hentDialog(dialogData.getId()).getHenvendelser();

        assertThat(henvendelser.size()).isEqualTo(1);
        HenvendelseData opprettet = henvendelser.get(0);
        assertThat(opprettet.getSendt()).isAfter(uniktTidspunkt);

        HenvendelseData forventet = henvendelseData
                .withId(henvendelseId)
                .withLestAvBruker(true)
                .withLestAvVeileder(true)
                .withSendt(opprettet.getSendt());
        assertThat(opprettet.getTekst()).isEqualTo(henvendelseData.getTekst());
        assertThat(opprettet).isEqualTo(forventet);
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
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("gammel").build());

        Date avslutningsdato = uniktTidspunkt();

        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("ny").build());

        List<DialogData> dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");
    }

    @Test
    public void hentKontorsperredeDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerNyereEnnUtmeldingstidspunkt() {
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("gammel").kontorsperreEnhetId("123").build());
        Date avslutningsdato = uniktTidspunkt();

        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("ny").kontorsperreEnhetId("123").build());

        List<DialogData> dialoger = dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");

    }

    @Test
    public void hentKontorsperredeDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerSomIkkeErKontorsperret() {
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("med_sperre").kontorsperreEnhetId("123").build());
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("uten_sperre").build());
        Date avslutningsdato = uniktTidspunkt();

        val dialoger = dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("med_sperre");
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

}
