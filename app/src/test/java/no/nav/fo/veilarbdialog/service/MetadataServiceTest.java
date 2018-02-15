package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.TestDataBuilder;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static no.nav.fo.veilarbdialog.domain.BooleanUpdateEnum.TRUE;
import static no.nav.fo.veilarbdialog.domain.DateUpdateEnum.NOW;
import static no.nav.fo.veilarbdialog.domain.DateUpdateEnum.NULL;
import static no.nav.fo.veilarbdialog.domain.DateUpdateEnum.NYESTE_HENVENDELSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MetadataServiceTest {

    private DialogDAO dialogDAO = mock(DialogDAO.class);
    private MetadataService metadataService = new MetadataService(dialogDAO);
    private ArgumentCaptor<DialogStatusOppdaterer> statusCaptor = ArgumentCaptor.forClass(DialogStatusOppdaterer.class);

    @Test
    public void ny_henvendelse_fra_bruker_endrer_eldste_uleste_for_veileder_og_venter_pa_nav() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);


        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getVenterPaNavSiden()).isEqualTo(NYESTE_HENVENDELSE);
        assertThat(status.getEldsteUlesteForVeileder()).isEqualTo(NYESTE_HENVENDELSE);

        DialogStatusOppdaterer forventet = new DialogStatusOppdaterer(dialogData.getId());
        forventet.setVenterPaNavSiden(NYESTE_HENVENDELSE);
        forventet.setEldsteUlesteForVeileder(NYESTE_HENVENDELSE);
        assertThat(status).isEqualTo(forventet);
    }

    @Test
    public void ny_henvendelse_fra_veileder_endrer_eldste_uleste_for_bruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraVeileder(dialogData, uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);
        DialogStatusOppdaterer oppdatert = getDialogDAOOppdaterStatusArg();

        assertThat(oppdatert.getEldsteUlesteForBruker()).isEqualTo(NYESTE_HENVENDELSE);

        DialogStatusOppdaterer forventet = new DialogStatusOppdaterer(dialogData.getId());
        forventet.setEldsteUlesteForBruker(NYESTE_HENVENDELSE);

        assertThat(oppdatert).isEqualTo(forventet);
    }

    @Test
    public void ny_henvendelse_pa_eksisterende_dialog_skal_resette_venter_paa_svar() {
        DialogData dialogData = getDialogData().withVenterPaSvarFraBrukerSiden(new Date());
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getVenterPaSvarFraBruker()).isEqualTo(NULL);

        DialogStatusOppdaterer forventetStatus = new DialogStatusOppdaterer(dialogData.getId());
        forventetStatus.setVenterPaSvarFraBruker(NULL);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void marker_som_lest_av_veileder_skal_sette_eldste_uleste_for_veileder_til_null() {
        DialogData dialogData = getDialogData().withEldsteUlesteTidspunktForVeileder(new Date());

        metadataService.markerSomLestAvVeileder(dialogData);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getEldsteUlesteForVeileder()).isEqualTo(NULL);
        assertThat(status.getLestAvVeilederTid()).isEqualTo(NOW);

        DialogStatusOppdaterer forventetStatus = new DialogStatusOppdaterer(dialogData.getId());
        forventetStatus.setEldsteUlesteForVeileder(NULL);
        forventetStatus.setLestAvVeilederTid(NOW);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void marker_som_lest_av_bruker_skal_sette_eldste_uleste_for_bruker_til_null() {
        DialogData dialogData = getDialogData();

        metadataService.markerSomLestAvBruker(dialogData);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getEldsteUlesteForBruker()).isEqualTo(NULL);
        assertThat(status.getLestAvBrukerTid()).isEqualTo(NOW);

        DialogStatusOppdaterer forventetStatus = new DialogStatusOppdaterer(dialogData.getId());
        forventetStatus.setEldsteUlesteForBruker(NULL);
        forventetStatus.setLestAvBrukerTid(NOW);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void oppdater_venter_pa_nav_siden_med_ferdigbehandlet_skal_sette_venter_pa_nav_siden_til_null() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(new Date());
        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, true);

        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getVenterPaNavSiden()).isEqualTo(NULL);

        DialogStatusOppdaterer forventetStatus = new DialogStatusOppdaterer(dialogData.getId());
        forventetStatus.setVenterPaNavSiden(NULL);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void nar_jeg_setter_venter_pa_nav_forventer_jeg_at_venter_pa_nav_blir_satt() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(null);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getVenterPaNavSiden()).isEqualTo(NOW);

        DialogStatusOppdaterer forventetStatus =new DialogStatusOppdaterer(dialogData.getId());
        forventetStatus.setVenterPaNavSiden(NOW);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void nar_jeg_fjerner_venter_pa_svar_forventer_jeg_at_venter_pa_svar_fra_bruker_er_null() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(new Date());

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogStatus);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getVenterPaSvarFraBruker()).isEqualTo(NULL);

        DialogStatusOppdaterer forventetStatus = new DialogStatusOppdaterer(dialogData.getId());
        forventetStatus.setVenterPaSvarFraBruker(NULL);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void naar_jeg_setter_venter_pa_svar_fra_bruker_forventer_jeg_at_venter_pa_svar_fra_bruker_blir_satt() {
        DialogData dialogData = getDialogData().withVenterPaSvarFraBrukerSiden(null);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), true, true);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogStatus);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getVenterPaSvarFraBruker()).isEqualTo(NOW);

        DialogStatusOppdaterer forventetStatus = new DialogStatusOppdaterer(dialogData.getId());
        forventetStatus.setVenterPaSvarFraBruker(NOW);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void nar_jeg_setter_dialog_til_historisk_forventer_jeg_at_dialogen_blir_historisk_og_alle_felter_blir_nullstilt() {
        DialogData dialogData = getDialogData();
        metadataService.settDialogTilHistorisk(dialogData);

        DialogStatusOppdaterer status = getDialogDAOOppdaterStatusArg();
        assertThat(status.getHistorisk()).isEqualTo(TRUE);

        DialogStatusOppdaterer forventeStatus = new DialogStatusOppdaterer(dialogData.getId());
        forventeStatus.setHistorisk(BooleanUpdateEnum.TRUE);
        forventeStatus.setVenterPaNavSiden(NULL);
        forventeStatus.setVenterPaSvarFraBruker(NULL);
        assertThat(status).isEqualTo(forventeStatus);
    }

    private DialogStatusOppdaterer getDialogDAOOppdaterStatusArg() {
        verify(dialogDAO, only()).oppdaterStatus(statusCaptor.capture());
        List<DialogStatusOppdaterer> allValues = statusCaptor.getAllValues();
        assertThat(allValues.size()).isEqualTo(1);
        return allValues.get(0);
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
