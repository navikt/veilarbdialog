package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.TestDataBuilder;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.UtilDAO;
import no.nav.fo.veilarbdialog.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MetadataServiceTest {

    private DialogDAO dialogDAO = mock(DialogDAO.class);
    private UtilDAO utilDAO = mock(UtilDAO.class);
    private MetadataService metadataService = new MetadataService(dialogDAO, utilDAO);
    private ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);

    @Test
    public void ny_henvendelse_fra_bruker_endrer_eldste_uleste_for_veileder_og_venter_pa_nav() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.venterPaNavSiden).isEqualTo(uniktTidspunkt);
        assertThat(status.eldsteUlesteForVeileder).isEqualTo(uniktTidspunkt);

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.settVenterPaNavSiden(uniktTidspunkt);
        forventetStatus.setUlesteMeldingerForVeileder(uniktTidspunkt);
        forventetStatus.setLestAvBrukerTid(uniktTidspunkt);

        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void ny_henvendelse_fra_veileder_endrer_eldste_uleste_for_bruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraVeileder(dialogData, uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);
        Status oppdatert = getDialogDAOOppdaterStatusArg();

        assertThat(oppdatert.eldsteUlesteForBruker).isEqualTo(uniktTidspunkt);
        assertThat(oppdatert.getLestAvVeilederTid()).isEqualTo(uniktTidspunkt);

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.eldsteUlesteForBruker = uniktTidspunkt;
        forventetStatus.setLestAvVeilederTid(uniktTidspunkt);

        assertThat(oppdatert).isEqualTo(forventetStatus);
    }

    @Test
    public void ny_henvendelse_pa_eksisterende_dialog_skal_resette_venter_paa_svar() {
        DialogData dialogData = getDialogData().withVenterPaSvarFraBrukerSiden(new Date());
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.venterPaSvarFraBruker).isNull();
        assertThat(status.getLestAvBrukerTid()).isEqualTo(uniktTidspunkt);

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.venterPaSvarFraBruker = null;
        forventetStatus.setLestAvBrukerTid(uniktTidspunkt);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void marker_som_lest_av_veileder_skal_sette_eldste_uleste_for_veileder_til_null() {
        DialogData dialogData = getDialogData().withEldsteUlesteTidspunktForVeileder(new Date());

        metadataService.markerSomLestAvVeileder(dialogData);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.eldsteUlesteForVeileder).isNull();

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.eldsteUlesteForVeileder = null;
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void marker_som_lest_av_bruker_skal_sette_eldste_uleste_for_bruker_til_null() {
        DialogData dialogData = getDialogData();

        metadataService.markerSomLestAvBruker(dialogData);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.eldsteUlesteForBruker).isNull();

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.eldsteUlesteForBruker = null;
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void oppdater_venter_pa_nav_siden_med_ferdigbehandlet_skal_sette_venter_pa_nav_siden_til_null() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(new Date());
        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, true);

        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.venterPaNavSiden).isNull();

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.venterPaNavSiden = null;
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void nar_jeg_setter_venter_pa_nav_forventer_jeg_at_venter_pa_nav_blir_satt() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(null);
        Date uniktTidspunkt = uniktTidspunkt();

        when(utilDAO.getTimestampFromDB()).thenReturn(uniktTidspunkt);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.venterPaNavSiden).isEqualTo(uniktTidspunkt);

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.settVenterPaNavSiden(uniktTidspunkt);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void nar_jeg_fjerner_venter_pa_svar_forventer_jeg_at_venter_pa_svar_fra_bruker_er_null() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(new Date());

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.venterPaSvarFraBruker).isNull();

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.venterPaSvarFraBruker = null;
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void naar_jeg_setter_venter_pa_svar_fra_bruker_forventer_jeg_at_venter_pa_svar_fra_bruker_blir_satt() {
        DialogData dialogData = getDialogData().withVenterPaSvarFraBrukerSiden(null);

        Date uniktTidspunkt = uniktTidspunkt();

        when(utilDAO.getTimestampFromDB()).thenReturn(uniktTidspunkt);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), true, true);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);

        Status status = getDialogDAOOppdaterStatusArg();
        assertThat(status.venterPaSvarFraBruker).isEqualTo(uniktTidspunkt);

        Status forventetStatus = MetadataService.getStatus(dialogData);
        forventetStatus.settVenterPaSvarFraBruker(uniktTidspunkt);
        assertThat(status).isEqualTo(forventetStatus);
    }

    @Test
    public void nar_jeg_setter_dialog_til_historisk_forventer_jeg_at_dialogen_blir_historisk_og_alle_felter_blir_nullstilt() {
        DialogData dialogData = getDialogData();

        Status status = new Status(dialogData.getId());
        status.setHistorisk(true);

        metadataService.settDialogTilHistorisk(dialogData);
        verify(dialogDAO, only()).oppdaterStatus(status);

    }

    private Status getDialogDAOOppdaterStatusArg() {
        verify(dialogDAO, only()).oppdaterStatus(statusCaptor.capture());
        List<Status> allValues = statusCaptor.getAllValues();
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