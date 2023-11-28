package no.nav.fo.veilarbdialog.db.dao;

import no.nav.fo.veilarbdialog.domain.DialogData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static org.assertj.core.api.Assertions.assertThat;

class StatusDAOTest extends BaseDAOTest {

    private static StatusDAO statusDAO;

    private static DialogDAO dialogDAO;

    @BeforeAll
    public static void setup() {
        dialogDAO = new DialogDAO(jdbc);
        statusDAO = new StatusDAO(jdbc);
    }

    @Test
    void markerSomLestAvVeileder_SkalSetteLestTidspunkt() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date forLest = Date.from(Instant.now().minusSeconds(5));
        statusDAO.markerSomLestAvVeileder(dialogData.getId(), new Date());
        Date etterLest = Date.from(Instant.now().plusSeconds(5));

        DialogData lest = dialogDAO.hentDialog(dialogData.getId());

        assertThat(lest.getLestAvVeilederTidspunkt()).isBetween(forLest, etterLest);
        assertThat(lest.getOppdatert()).isBetween(forLest, etterLest);
        assertThat(lest.getSisteUlestAvVeilederTidspunkt()).isNull();

        DialogData forventet = dialogData.withOppdatert(lest.getOppdatert())
                .withSisteUlestAvVeilederTidspunkt(null)
                .withLestAvVeilederTidspunkt(lest.getLestAvVeilederTidspunkt());

        assertThat(lest).isEqualTo(forventet);
    }

    @Test
    void markerSomLestAvBruker() {
        DialogData dialogData = oppretDialogMedStatuser();

        Date forLest = Date.from(Instant.now().minusSeconds(5));
        statusDAO.markerSomLestAvBruker(dialogData.getId());
        Date etterLest = Date.from(Instant.now().plusSeconds(5));

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

        Date before = Date.from(Instant.now().minusSeconds(5));
        statusDAO.setVenterPaNavTilNaa(dialogData.getId());
        Date after = Date.from(Instant.now().plusSeconds(5));

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

        Date before = Date.from(Instant.now().minusSeconds(5));
        statusDAO.setVenterPaSvarFraBrukerTilNaa(dialogData.getId());
        Date after = Date.from(Instant.now().plusSeconds(5));

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

        Date before = Date.from(Instant.now().minusSeconds(5));
        statusDAO.setVenterPaNavTilNull(dialogData.getId());
        Date after = Date.from(Instant.now().plusSeconds(5));

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

        Date before = Date.from(Instant.now().minusSeconds(5));
        statusDAO.setVenterPaSvarFraBrukerTilNull(dialogData.getId());
        Date after = Date.from(Instant.now().plusSeconds(5));

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

        Date elsteulest = Date.from(Instant.now().minusSeconds(5));
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

        Date elsteUlesteForVeileder = Date.from(Instant.now().minusSeconds(5));
        Date venterPaNavSiden = Date.from(Instant.now().minusSeconds(4));
        statusDAO.setNyMeldingFraBruker(dialogData.getId(), elsteUlesteForVeileder, venterPaNavSiden);

        DialogData nyMeldingFraBruker = dialogDAO.hentDialog(dialogData.getId());
        assertThat(nyMeldingFraBruker.getVenterPaNavSiden()).isEqualTo(venterPaNavSiden);
        assertThat(nyMeldingFraBruker.getVenterPaSvarFraBrukerSiden()).isNull();
        assertThat(nyMeldingFraBruker.getSisteUlestAvVeilederTidspunkt()).isEqualTo(elsteUlesteForVeileder);

        DialogData forventet = dialogData
                .withOppdatert(nyMeldingFraBruker.getOppdatert())
                .withVenterPaNavSiden(venterPaNavSiden)
                .withVenterPaSvarFraBrukerSiden(null)
                .withSisteUlestAvVeilederTidspunkt(elsteUlesteForVeileder);

        assertThat(nyMeldingFraBruker).isEqualTo(forventet);
    }

    @Test
    void setHistorisk() {
        DialogData dialogData = oppretDialogMedStatuser();
        statusDAO.setHistorisk(dialogData.getId());

        DialogData historisk = dialogDAO.hentDialog(dialogData.getId());
        assertThat(historisk.isHistorisk()).isTrue();
        assertThat(historisk.getVenterPaSvarFraBrukerSiden()).isNull();
        assertThat(historisk.getVenterPaNavSiden()).isNull();
    }

    private DialogData oppretDialogMedStatuser() {
        long id = dialogDAO.opprettDialog(nyDialog()).getId();
        statusDAO.markerSomLestAvBruker(id);
        statusDAO.markerSomLestAvVeileder(id, new Date());
        statusDAO.setEldsteUlesteForBruker(id, new Date());
        statusDAO.setNyMeldingFraBruker(id, new Date(), new Date());
        statusDAO.setVenterPaSvarFraBrukerTilNaa(id);
        return dialogDAO.hentDialog(id);
    }

}
