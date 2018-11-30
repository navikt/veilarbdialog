package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.IntegationTest;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static org.assertj.core.api.Assertions.assertThat;

class StatusDAOTest extends IntegationTest {

    @Inject
    private StatusDAO statusDAO;

    @Inject
    private DialogDAO dialogDAO;

    @BeforeAll
    public static void addSpringBeans() {
        initSpringContext(Arrays.asList(StatusDAO.class, DialogDAO.class));
    }

    @Test
    void markerSomLestAvVeilederSkalSetteLestTidspunkt() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date forLest = uniktTidspunkt();
        statusDAO.markerSomLestAvVeileder(dialogData.getId());
        Date etterLest = uniktTidspunkt();

        DialogData lest = dialogDAO.hentDialog(dialogData.getId());

        assertThat(lest.getLestAvVeilederTidspunkt()).isBetween(forLest, etterLest);
        assertThat(lest.getOppdatert()).isBetween(forLest, etterLest);
        assertThat(lest.getEldsteUlesteTidspunktForVeileder()).isNull();

        DialogData forventet = dialogData.withOppdatert(lest.getOppdatert())
                .withEldsteUlesteTidspunktForVeileder(null)
                .withLestAvVeilederTidspunkt(lest.getLestAvVeilederTidspunkt());

        assertThat(lest).isEqualTo(forventet);
    }

    @Test
    void markerSomLestAvBruker() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date forLest = uniktTidspunkt();
        statusDAO.markerSomLestAvBruker(dialogData.getId());
        Date etterLest = uniktTidspunkt();

        DialogData lest = dialogDAO.hentDialog(dialogData.getId());

        assertThat(lest.getLestAvBrukerTidspunkt()).isBetween(forLest, etterLest);
        assertThat(lest.getOppdatert()).isBetween(forLest, etterLest);
        assertThat(lest.getEldsteUlesteTidspunktForBruker()).isNull();

        DialogData forventet = dialogData.withOppdatert(lest.getOppdatert())
                .withEldsteUlesteTidspunktForBruker(null)
                .withLestAvBrukerTidspunkt(lest.getLestAvBrukerTidspunkt());

        assertThat(lest).isEqualTo(forventet);
    }

    @Test
    void setVenterPaNavTilNaa() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date before = uniktTidspunkt();
        statusDAO.setVenterPaNavTilNaa(dialogData.getId());
        Date after = uniktTidspunkt();

        DialogData venterPaNav = dialogDAO.hentDialog(dialogData.getId());
        assertThat(venterPaNav.getVenterPaNavSiden()).isBetween(before, after);
        assertThat(venterPaNav.getOppdatert()).isBetween(before, after);

        DialogData forventet = dialogData
                .withOppdatert(venterPaNav.getOppdatert())
                .withVenterPaNavSiden(venterPaNav.getVenterPaNavSiden());

        assertThat(venterPaNav).isEqualTo(forventet);
    }

    @Test
    void setVenterPaSvarFraBrukerTilNaa() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date before = uniktTidspunkt();
        statusDAO.setVenterPaSvarFraBrukerTilNaa(dialogData.getId());
        Date after = uniktTidspunkt();

        DialogData venterPaBruker = dialogDAO.hentDialog(dialogData.getId());
        assertThat(venterPaBruker.getVenterPaSvarFraBrukerSiden()).isBetween(before, after);
        assertThat(venterPaBruker.getOppdatert()).isBetween(before, after);

        DialogData forventet = dialogData
                .withOppdatert(venterPaBruker.getOppdatert())
                .withVenterPaSvarFraBrukerSiden(venterPaBruker.getVenterPaSvarFraBrukerSiden());

        assertThat(venterPaBruker).isEqualTo(forventet);
    }

    @Test
    void setVenterPaNavTilNull() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date before = uniktTidspunkt();
        statusDAO.setVenterPaNavTilNull(dialogData.getId());
        Date after = uniktTidspunkt();

        DialogData venterIkkePaNav = dialogDAO.hentDialog(dialogData.getId());
        assertThat(venterIkkePaNav.getVenterPaNavSiden()).isNull();
        assertThat(venterIkkePaNav.getOppdatert()).isBetween(before, after);

        DialogData forventet = dialogData
                .withOppdatert(venterIkkePaNav.getOppdatert())
                .withVenterPaNavSiden(venterIkkePaNav.getVenterPaNavSiden());

        assertThat(venterIkkePaNav).isEqualTo(forventet);
    }

    @Test
    void setVenterPaSvarFraBrukerTilNull() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date before = uniktTidspunkt();
        statusDAO.setVenterPaSvarFraBrukerTilNull(dialogData.getId());
        Date after = uniktTidspunkt();

        DialogData venterIkkePaNav = dialogDAO.hentDialog(dialogData.getId());
        assertThat(venterIkkePaNav.getVenterPaSvarFraBrukerSiden()).isNull();
        assertThat(venterIkkePaNav.getOppdatert()).isBetween(before, after);

        DialogData forventet = dialogData
                .withOppdatert(venterIkkePaNav.getOppdatert())
                .withVenterPaSvarFraBrukerSiden(venterIkkePaNav.getVenterPaSvarFraBrukerSiden());

        assertThat(venterIkkePaNav).isEqualTo(forventet);
    }

    @Test
    void setEldsteUlesteForBruker() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date elsteulest = uniktTidspunkt();
        statusDAO.setEldsteUlesteForBruker(dialogData.getId(), elsteulest);

        DialogData medulest = dialogDAO.hentDialog(dialogData.getId());
        assertThat(medulest.getEldsteUlesteTidspunktForBruker()).isEqualTo(elsteulest);
        assertThat(medulest.getOppdatert()).isAfter(elsteulest);

        DialogData forventet = dialogData
                .withEldsteUlesteTidspunktForBruker(medulest.getEldsteUlesteTidspunktForBruker())
                .withOppdatert(medulest.getOppdatert());

        assertThat(medulest).isEqualTo(forventet);
    }

    @Test
    void setNyMeldingFraBruker() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date elsteUlesteForVeileder = uniktTidspunkt();
        Date venterPaNavSiden = uniktTidspunkt();
        statusDAO.setNyMeldingFraBruker(dialogData.getId(), elsteUlesteForVeileder, venterPaNavSiden);
        Date after = uniktTidspunkt();

        DialogData nyMeldingFraBruker = dialogDAO.hentDialog(dialogData.getId());
        assertThat(nyMeldingFraBruker.getOppdatert()).isBetween(venterPaNavSiden, after);
        assertThat(nyMeldingFraBruker.getVenterPaNavSiden()).isEqualTo(venterPaNavSiden);
        assertThat(nyMeldingFraBruker.getVenterPaSvarFraBrukerSiden()).isNull();
        assertThat(nyMeldingFraBruker.getEldsteUlesteTidspunktForVeileder()).isEqualTo(elsteUlesteForVeileder);

        DialogData forventet = dialogData
                .withOppdatert(nyMeldingFraBruker.getOppdatert())
                .withVenterPaNavSiden(venterPaNavSiden)
                .withVenterPaSvarFraBrukerSiden(null)
                .withEldsteUlesteTidspunktForVeileder(elsteUlesteForVeileder);

        assertThat(nyMeldingFraBruker).isEqualTo(forventet);
    }

    @Test
    void setHistorisk() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date before = uniktTidspunkt();
        statusDAO.setHistorisk(dialogData.getId());
        Date after = uniktTidspunkt();

        DialogData historisk = dialogDAO.hentDialog(dialogData.getId());
        assertThat(historisk.getOppdatert()).isBetween(before, after);
        assertThat(historisk.isHistorisk()).isTrue();
        assertThat(historisk.getVenterPaSvarFraBrukerSiden()).isNull();
        assertThat(historisk.getVenterPaNavSiden()).isNull();
    }

    private DialogData oppretDialogMedStatuser() {
        long id = dialogDAO.opprettDialog(nyDialog()).getId();
        statusDAO.markerSomLestAvBruker(id);
        statusDAO.markerSomLestAvVeileder(id);
        statusDAO.setEldsteUlesteForBruker(id, new Date());
        statusDAO.setNyMeldingFraBruker(id, new Date(), new Date());
        statusDAO.setVenterPaSvarFraBrukerTilNaa(id);
        return dialogDAO.hentDialog(id);
    }

}
