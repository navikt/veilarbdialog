package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.TestDataBuilder;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Date;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;

class StatusDAOTest extends IntegrasjonsTest {
    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private StatusDAO statusDAO;

    @Test
    public void skalSetteVenterPaNavTilNyttTidspunkt() {
        DialogData nyDialog = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(nyDialog);

        Date uniktTidspunkt = uniktTidspunkt();
        DialogData originalDialog = dialogDAO.hentDialog(id);

        assertThat(originalDialog.getVenterPaNav()).isNull();

        statusDAO.oppdaterVenterPaNav(id, true);
        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getVenterPaNav()).isAfter(uniktTidspunkt);

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
                .withVenterPaNav(oppdatertDialog.getVenterPaNav())
        );
    }

    @Test
    public void skalIkkeOppdatereAlleredeSattVenterPaNav() {
        DialogData nyDialog = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(nyDialog);

        statusDAO.oppdaterVenterPaNav(id, true);
        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterVenterPaNav(id, true);
        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getVenterPaNav()).isBefore(uniktTidspunkt);

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
        );
    }

    @Test
    public void skalFjerneVenterPaNav() {
        DialogData nyDialog = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(nyDialog);

        statusDAO.oppdaterVenterPaNav(id, true);
        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterVenterPaNav(id, false);

        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getVenterPaNav()).isNull();

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withVenterPaNav(oppdatertDialog.getVenterPaNav())
                .withOppdatert(oppdatertDialog.getOppdatert())
        );
    }

    @Test
    public void skalSetteVenterPaSvarFraBrukerTilNyttTidspunkt() {
        DialogData nyDialog = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(nyDialog);

        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterVenterPaSvarFraBruker(id, true);
        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getVenterPaSvarFraBruker()).isAfter(uniktTidspunkt);

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
                .withVenterPaSvarFraBruker(oppdatertDialog.getVenterPaSvarFraBruker())
        );
    }

    @Test
    public void skalIkkeOppdatereAlleredeSattVenterPaSvarFraBruker() {
        DialogData nyDialog = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(nyDialog);

        statusDAO.oppdaterVenterPaSvarFraBruker(id, true);

        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterVenterPaSvarFraBruker(id, true);
        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getVenterPaSvarFraBruker()).isBefore(uniktTidspunkt);
        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
        );
    }

    @Test
    public void skalFjerneVenterPaSvarFraBruker() {
        DialogData nyDialog = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(nyDialog);

        statusDAO.oppdaterVenterPaSvarFraBruker(id, true);

        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterVenterPaSvarFraBruker(id, false);

        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getVenterPaSvarFraBruker()).isNull();
        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
                .withVenterPaSvarFraBruker(oppdatertDialog.getVenterPaSvarFraBruker())
        );
    }

    @Test
    public void skalOppdaterStatusForHenvendelseFraBruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(dialogData);

        HenvendelseData henvendelseData = TestDataBuilder.nyHenvendelse(id, dialogData.getAktorId(), AvsenderType.BRUKER);

        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);

        DialogData oppdatertDialog = dialogDAO.hentDialog(id);
        assertThat(oppdatertDialog.getVenterPaNav()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getUlesteMeldingerForVeileder()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
                .withVenterPaNav(oppdatertDialog.getVenterPaNav())
                .withUlesteMeldingerForVeileder(oppdatertDialog.getUlesteMeldingerForVeileder())
        );
    }

    @Test
    public void skalOppdatereOppdatertMenIkkeVenterPaNavForAleredeSat() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(dialogData);

        HenvendelseData henvendelseData = TestDataBuilder.nyHenvendelse(id, dialogData.getAktorId(), AvsenderType.BRUKER);
        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);

        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);
        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getUlesteMeldingerForVeileder()).isBefore(uniktTidspunkt);
        assertThat(oppdatertDialog.getVenterPaNav()).isBefore(uniktTidspunkt);
        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
        );
    }

    @Test
    public void skalOppdaterStatusForHenvendelseFraVeileder() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(dialogData);

        HenvendelseData henvendelseData = TestDataBuilder.nyHenvendelse(id, dialogData.getAktorId(), AvsenderType.VEILEDER);

        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);
        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getUlesteMeldingerForBruker()).isAfter(uniktTidspunkt);
        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
                .withUlesteMeldingerForBruker(oppdatertDialog.getUlesteMeldingerForBruker())
        );
    }

    @Test
    public void skalOppdatereOppdatertMenIkkeUlesteForBruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(dialogData);

        statusDAO.oppdaterVenterPaNav(id, true);

        HenvendelseData henvendelseData = TestDataBuilder.nyHenvendelse(id, dialogData.getAktorId(), AvsenderType.VEILEDER);
        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);

        DialogData originalDialog = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);
        DialogData oppdatertDialog = dialogDAO.hentDialog(id);

        assertThat(oppdatertDialog.getUlesteMeldingerForBruker()).isBefore(uniktTidspunkt);
        assertThat(oppdatertDialog.getOppdatert()).isAfter(uniktTidspunkt);

        assertThat(oppdatertDialog).isEqualTo(originalDialog
                .withOppdatert(oppdatertDialog.getOppdatert())
        );
    }

    @Test
    public void skalMarkereSomLestAvBruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(dialogData);

        HenvendelseData henvendelseData = TestDataBuilder.nyHenvendelse(id, dialogData.getAktorId(), AvsenderType.VEILEDER);
        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);

        DialogData original = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.markerSomLestAvBruker(id);
        DialogData oppdatert = dialogDAO.hentDialog(id);

        assertThat(original.getUlesteMeldingerForBruker()).isNotNull();
        assertThat(oppdatert.getUlesteMeldingerForBruker()).isNull();
        assertThat(oppdatert.getOppdatert()).isAfter(uniktTidspunkt);

        assertThat(oppdatert).isEqualTo(original
                .withOppdatert(oppdatert.getOppdatert())
                .withUlesteMeldingerForBruker(null)
        );
    }

    @Test
    public void skalMarkereSomLestAvVeileder() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        long id = dialogDAO.opprettDialog(dialogData);

        HenvendelseData henvendelseData = TestDataBuilder.nyHenvendelse(id, dialogData.getAktorId(), AvsenderType.BRUKER);
        statusDAO.oppdaterStatusForNyHenvendelse(henvendelseData);

        DialogData original = dialogDAO.hentDialog(id);
        Date uniktTidspunkt = uniktTidspunkt();

        statusDAO.markerSomLestAvVeileder(id);
        DialogData oppdatert = dialogDAO.hentDialog(id);

        assertThat(original.getUlesteMeldingerForVeileder()).isNotNull();
        assertThat(oppdatert.getUlesteMeldingerForVeileder()).isNull();
        assertThat(oppdatert.getOppdatert()).isAfter(uniktTidspunkt);

        assertThat(oppdatert).isEqualTo(original
                .withOppdatert(oppdatert.getOppdatert())
                .withUlesteMeldingerForVeileder(null)
        );
    }

    @SneakyThrows
    private Date uniktTidspunkt() {
        sleep(1);
        Date tidspunkt = new Date();
        sleep(1);
        return tidspunkt;
    }
}