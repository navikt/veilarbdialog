package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.AktivitetId;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@Transactional
@Sql(
        scripts = "/db/testdata/slett_alle_dialoger.sql",
        executionPhase = BEFORE_TEST_METHOD
)
public class DialogDAOTest {

    private static final String AKTOR_ID_1234 = "1234";

    @Autowired
    private DialogDAO dialogDAO;

    @Test
    public void kan_opprette_dialog() {
        DialogData dialog = nyDialog();
        DialogData dialogData = dialogDAO.opprettDialog(dialog);
        assertThat(dialogData.getOppdatert()).isNotNull();
        assertThat(dialogData.getOpprettetDato()).isNotNull();

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

        long henvendelseId = dialogDAO.opprettHenvendelse(henvendelseData).getId();
        List<HenvendelseData> henvendelser = dialogDAO.hentDialog(dialogData.getId()).getHenvendelser();

        assertThat(henvendelser).hasSize(1);
        HenvendelseData opprettet = henvendelser.get(0);

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
        var aktivitetId = AktivitetId.of("aktivitetId");
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isEmpty();
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().aktivitetId(aktivitetId).build());
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isPresent();
    }

    @Test
    public void kanHenteDialogPaaArenaAktivitetId() {
        var aktivitetId = AktivitetId.of("ARENATA123");
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
        var dialog = nyDialog(AKTOR_ID_1234).toBuilder().opprettetDato(DateTime.now().minusSeconds(5).toDate()).overskrift("gammel").build();
        dialogDAO.opprettDialog(dialog);

        Date avslutningsdato = new Date();

        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).withOpprettetDato(DateTime.now().plusSeconds(5).toDate()).toBuilder().overskrift("ny").build());

        List<DialogData> dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");
    }

    @Test
    public void hentKontorsperredeDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerNyereEnnUtmeldingstidspunkt() {
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().opprettetDato(DateTime.now().minusSeconds(5).toDate()).overskrift("gammel").kontorsperreEnhetId("123").build());
        Date avslutningsdato = new Date();

        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("ny").opprettetDato(DateTime.now().plusSeconds(5).toDate()).kontorsperreEnhetId("123").build());

        List<DialogData> dialoger = dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");

    }

    @Test
    public void hentKontorsperredeDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerSomIkkeErKontorsperret() {
        var opprettet = DateTime.now().minusSeconds(5).toDate();
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("med_sperre").opprettetDato(opprettet).kontorsperreEnhetId("123").build());
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID_1234).toBuilder().overskrift("uten_sperre").opprettetDato(opprettet).build());

        var dialoger = dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(AKTOR_ID_1234, new Date());
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


    @Test
    public void skalHenteBrukereMedAktiveDialoger() {
        opprettNyDialog("1", false);
        opprettNyDialog("1", false);
        opprettNyDialog("2", false);
        opprettNyDialog("2", true);
        opprettNyDialog("3", true);
        opprettNyDialog("4", true);
        opprettNyDialog("4", true);
        opprettNyDialog("5", false);

        List<String> brukere = dialogDAO.hentAktorIderTilBrukereMedAktiveDialoger();

        assertThat(brukere)
                .hasSize(3)
                .containsExactlyInAnyOrder("1", "2", "5");
    }

    private DialogData opprettNyDialog(String aktorId, boolean historisk) {
        return dialogDAO.opprettDialog(nyDialog(aktorId).withHistorisk(historisk));
    }

    private DialogData opprettNyDialog(String aktorId) {
        return opprettNyDialog(aktorId, false);
    }

}
