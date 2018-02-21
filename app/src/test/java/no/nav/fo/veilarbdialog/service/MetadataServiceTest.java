package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.TestDataBuilder;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.StatusDAO;
import no.nav.fo.veilarbdialog.domain.*;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.*;

class MetadataServiceTest {

    private StatusDAO statusDAO = mock(StatusDAO.class);
    private DialogDAO dialogDAO = mock(DialogDAO.class);
    private MetadataService metadataService = new MetadataService(statusDAO, dialogDAO);

    @Test
    public void ny_henvendelse_fra_bruker_kaller_set_ny_melding_fra_bruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt());

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        verify(statusDAO, only()).setNyMeldingFraBruker(dialogData.getId(), henvendelseData.getSendt(), henvendelseData.getSendt());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void ny_henvendelse_fra_bruker_pa_dialog_med_venter_pa_nav_skal_kalle_ny_melding_fra_bruker_med_gammel_venter_pa_nav_tidspunkt() {
        DialogData dialogData = TestDataBuilder.nyDialog().withVenterPaNavSiden(uniktTidspunkt());
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        verify(statusDAO, only()).setNyMeldingFraBruker(dialogData.getId(), uniktTidspunkt, dialogData.getVenterPaNavSiden());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void ny_henvendelse_fra_bruker_skal_ikke_endre_alerede_satt() {
        DialogData dialogData = TestDataBuilder.nyDialog()
                .withVenterPaNavSiden(uniktTidspunkt())
                .withEldsteUlesteTidspunktForVeileder(uniktTidspunkt());

        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt());

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        verify(statusDAO, only()).setNyMeldingFraBruker(dialogData.getId(), dialogData.getEldsteUlesteTidspunktForVeileder(), dialogData.getVenterPaNavSiden());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void ny_henvendelse_fra_veileder_endrer_eldste_uleste_for_bruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        HenvendelseData henvendelseData = nyHenvendelseFraVeileder(dialogData, uniktTidspunkt());

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        verify(statusDAO, only()).setEldsteUlesteForBruker(dialogData.getId(), henvendelseData.getSendt());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void ny_henvendelse_fra_veileder_med_alerede_ules_melding_skal_ikke_endre_tidspunkt() {
        DialogData dialogData = TestDataBuilder.nyDialog().withEldsteUlesteTidspunktForBruker(uniktTidspunkt());
        HenvendelseData henvendelseData = nyHenvendelseFraVeileder(dialogData, uniktTidspunkt());

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        verify(statusDAO, only()).setEldsteUlesteForBruker(dialogData.getId(), dialogData.getEldsteUlesteTidspunktForBruker());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void marker_som_lest_av_veileder_skal_sette_eldste_uleste_for_veileder_til_null() {
        DialogData dialogData = getDialogData().withEldsteUlesteTidspunktForVeileder(new Date());

        metadataService.markerSomLestAvVeileder(dialogData);

        verify(statusDAO, only()).markerSomLestAvVeileder(dialogData.getId());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void marker_som_lest_av_bruker_skal_sette_eldste_uleste_for_bruker_til_null() {
        DialogData dialogData = getDialogData();

        metadataService.markerSomLestAvBruker(dialogData);

        verify(statusDAO, only()).markerSomLestAvBruker(dialogData.getId());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void oppdater_venter_pa_nav_siden_med_ferdigbehandlet_skal_sette_venter_pa_nav_siden_til_null() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(new Date());
        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, true);

        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        verify(statusDAO, only()).setVenterPaNavTilNull(dialogData.getId());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void nar_jeg_setter_venter_pa_nav_forventer_jeg_at_venter_pa_nav_blir_satt() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(null);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        verify(statusDAO, only()).setVenterPaNavTilNaa(dialogData.getId());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void nar_jeg_fjerner_venter_pa_svar_forventer_jeg_at_venter_pa_svar_fra_bruker_er_null() {
        DialogData dialogData = getDialogData();

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogStatus);

        verify(statusDAO, only()).setVenterPaSvarFraBrukerTilNull(dialogData.getId());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void naar_jeg_setter_venter_pa_svar_fra_bruker_forventer_jeg_at_venter_pa_svar_fra_bruker_blir_satt() {
        DialogData dialogData = getDialogData().withVenterPaSvarFraBrukerSiden(null);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), true, true);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogStatus);

        verify(statusDAO, only()).setVenterPaSvarFraBrukerTilNaa(dialogData.getId());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    @Test
    public void nar_jeg_setter_dialog_til_historisk_forventer_jeg_at_statusdao_set_dialog_til_historisk_blir_kalt() {
        DialogData dialogData = getDialogData();
        metadataService.settDialogTilHistorisk(dialogData);
        verify(statusDAO, only()).setHistorisk(dialogData.getId());
        verify(dialogDAO, only()).hentDialog(dialogData.getId());
    }

    private DialogData getDialogData() {
        return TestDataBuilder.nyDialog()
                .withEldsteUlesteTidspunktForBruker(new Date())
                .withEldsteUlesteTidspunktForVeileder(new Date())
                .withVenterPaNavSiden(new Date())
                .withVenterPaSvarFraBrukerSiden(new Date())
                .withHistorisk(false);
    }

    private HenvendelseData nyHenvendelseFraBruker(DialogData dialogData, Date uniktTidspunkt) {
        return TestDataBuilder
                .nyHenvendelse(dialogData.getId(), dialogData.getAktorId(), AvsenderType.BRUKER)
                .withSendt(uniktTidspunkt);
    }


    private HenvendelseData nyHenvendelseFraVeileder(DialogData dialogData, Date uniktTidspunkt) {
        return TestDataBuilder
                .nyHenvendelse(dialogData.getId(), dialogData.getAktorId(), AvsenderType.VEILEDER)
                .withSendt(uniktTidspunkt);
    }

    @SneakyThrows
    private Date uniktTidspunkt() {
        sleep(1);
        Date tidspunkt = new Date();
        sleep(1);
        return tidspunkt;
    }
}
