package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.AktivitetId;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static org.assertj.core.api.Assertions.assertThat;


class DialogDAOTest extends BaseDAOTest {

    private static DialogDAO dialogDAO;

    @BeforeAll
    public static void setup() {
       dialogDAO = new DialogDAO(jdbc);
    }

    @Test
     void kan_opprette_dialog() {
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
     void kanHenteDialogerPaaAktorId() {
        String aktorId = AktorIdProvider.getNext();

        DialogData dialogData = nyDialog(aktorId);
        dialogDAO.opprettDialog(dialogData);

        List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
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
     void kanHentDialogPaDialogId() {
       String aktorId = AktorIdProvider.getNext();

        DialogData dialogData = nyDialog(aktorId);
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
     void kanOppretteHenvendelse() {
       String aktorId = AktorIdProvider.getNext();
        DialogData dialogData = opprettNyDialog(aktorId);
        HenvendelseData henvendelseData = nyHenvendelse(dialogData.getId(), aktorId, AvsenderType.BRUKER);

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
     void kanHenteDialogPaaAktivitetId() {
       String aktorId = AktorIdProvider.getNext();
        var aktivitetId = AktivitetId.of("aktivitetId");
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isEmpty();
        dialogDAO.opprettDialog(nyDialog(aktorId).toBuilder().aktivitetId(aktivitetId).build());
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isPresent();
    }

    @Test
     void kanHenteDialogPaaArenaAktivitetId() {
       String aktorId = AktorIdProvider.getNext();
        var aktivitetId = AktivitetId.of("ARENATA123");
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isEmpty();
        dialogDAO.opprettDialog(nyDialog(aktorId).toBuilder().aktivitetId(aktivitetId).build());
        assertThat(dialogDAO.hentDialogForAktivitetId(aktivitetId)).isPresent();
    }

    @Test
     void hentDialogerSomSkalAvsluttesForAktorIdTarIkkeMedAlleredeHistoriske() {
       String aktorId = AktorIdProvider.getNext();
       UUID periodeId = UUID.randomUUID();

        DialogData dialog = nyDialog(aktorId)
                .toBuilder()
                .overskrift("ny")
                .oppfolgingsperiode(periodeId)
                .build();

        DialogData historiskDialog = nyDialog(aktorId)
                .toBuilder()
                .historisk(true)
                .overskrift("historisk")
                .oppfolgingsperiode(periodeId)
                .build();

        dialogDAO.opprettDialog(dialog);
        dialogDAO.opprettDialog(historiskDialog);

        List<DialogData> dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktorId, periodeId);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("ny");
    }

    @Test
     void hentDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerForEnAnnenOppfolgingsperiode() {
       String aktorId = AktorIdProvider.getNext();
        UUID gammelPeriodeId = UUID.randomUUID();
        UUID nyPeriodeId = UUID.randomUUID();
        var dialog = nyDialog(aktorId).toBuilder().oppfolgingsperiode(gammelPeriodeId).opprettetDato(Date.from(Instant.now().minusSeconds(5))).overskrift("gammel").build();
        dialogDAO.opprettDialog(dialog);

        dialogDAO.opprettDialog(nyDialog(aktorId).withOppfolgingsperiode(nyPeriodeId).withOpprettetDato(Date.from(Instant.now().plusSeconds(5))).withOverskrift("ny"));

        List<DialogData> dialoger = dialogDAO.hentDialogerSomSkalAvsluttesForAktorId(aktorId, gammelPeriodeId);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");
    }

    @Test
     void hentKontorsperredeDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerNyereEnnUtmeldingstidspunkt() {
       String aktorId = AktorIdProvider.getNext();
        dialogDAO.opprettDialog(nyDialog(aktorId).toBuilder().opprettetDato(Date.from(Instant.now().minusSeconds(5))).overskrift("gammel").kontorsperreEnhetId("123").build());
        Date avslutningsdato = new Date();

        dialogDAO.opprettDialog(nyDialog(aktorId).toBuilder().overskrift("ny").opprettetDato(Date.from(Instant.now().plusSeconds(5))).kontorsperreEnhetId("123").build());

        List<DialogData> dialoger = dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(aktorId, avslutningsdato);
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("gammel");

    }

    @Test
     void hentKontorsperredeDialogerSomSkalAvsluttesForAktorIdTarIkkeMedDialogerSomIkkeErKontorsperret() {
       String aktorId = AktorIdProvider.getNext();
        var opprettet = Date.from(Instant.now().minusSeconds(5));
        dialogDAO.opprettDialog(nyDialog(aktorId).toBuilder().overskrift("med_sperre").opprettetDato(opprettet).kontorsperreEnhetId("123").build());
        dialogDAO.opprettDialog(nyDialog(aktorId).toBuilder().overskrift("uten_sperre").opprettetDato(opprettet).build());

        var dialoger = dialogDAO.hentKontorsperredeDialogerSomSkalAvsluttesForAktorId(aktorId, new Date());
        assertThat(dialoger).hasSize(1);
        assertThat(dialoger.get(0).getOverskrift()).isEqualTo("med_sperre");
    }

    @Test
     void skalIkkeLoggeTekstInnhold() {
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
     void skalHenteBrukereMedAktiveDialoger() {
       List<String>  before = dialogDAO.hentAktorIderTilBrukereMedAktiveDialoger();
        opprettNyDialog("1", false);
        opprettNyDialog("1", false);
        opprettNyDialog("2", false);
        opprettNyDialog("2", true);
        opprettNyDialog("3", true);
        opprettNyDialog("4", true);
        opprettNyDialog("4", true);
        opprettNyDialog("5", false);

        List<String> after = dialogDAO.hentAktorIderTilBrukereMedAktiveDialoger();

       after.removeAll(before);

       assertThat(after)
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
