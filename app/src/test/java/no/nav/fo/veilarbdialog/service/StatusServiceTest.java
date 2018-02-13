package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.TestDataBuilder;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class StatusServiceTest {

    private DialogDAO dialogDAO = mock(DialogDAO.class);
    private StatusService statusService = new StatusService(dialogDAO);

    @Test
    public void nyHenvendelseFraBrukerEndrerEldsteUlesteForVeilederOgVenterPaNav() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        Status original = StatusService.getStatus(dialogData);

        statusService.nyHenvendelse(dialogData, henvendelseData);
        verify(dialogDAO, only()).oppdaterStatus(argThat(opdatert -> verifyEldsteUlesteOgVenterPaNavEndret(original, uniktTidspunkt, opdatert)));
    }

    private boolean verifyEldsteUlesteOgVenterPaNavEndret(Status original, Date tidspunkt, Status oppdatert) {
        assertThat(oppdatert.eldsteUlesteForVeileder).isEqualTo(tidspunkt);
        assertThat(oppdatert.venterPaNavSiden).isAfter(tidspunkt);

        original.eldsteUlesteForVeileder = oppdatert.eldsteUlesteForVeileder;
        original.venterPaNavSiden = oppdatert.venterPaNavSiden;
        assertThat(oppdatert).isEqualTo(original);
        return true;
    }

    @Test
    public void nyHenvendelseFraVeilederEndrerEldsteUlesteForBruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = new Date();
        HenvendelseData henvendelseData = nyHenvendelseFraVeileder(dialogData, uniktTidspunkt);

        Status status = StatusService.getStatus(dialogData);
        status.eldsteUlesteForBruker = uniktTidspunkt;

        statusService.nyHenvendelse(dialogData, henvendelseData);
        verify(dialogDAO).oppdaterStatus(status);
    }

    @Test
    public void nyHenvendelsePaEksisterendeDialogSkalResetteVenterPaaSvar() {
        DialogData dialogData = getDialogData();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        Status status = StatusService.getStatus(dialogData);
        status.venterPaSvarFraBruker = null;

        statusService.nyHenvendelse(dialogData, henvendelseData);
        verify(dialogDAO).oppdaterStatus(status);
    }

    @Test
    public void markerSomLestAvVeilederSkalSetteEldsteUlesteForVeilederTilNull() {
        DialogData dialogData = getDialogData();
        Status status = StatusService.getStatus(dialogData);
        status.eldsteUlesteForVeileder = null;

        statusService.markerSomLestAvVeileder(dialogData);

        verify(dialogDAO, Mockito.only()).oppdaterStatus(status);
    }

    @Test
    public void markerSomLestAvBrukerSkalSetteEldsteUlesteForBrukerTilNull() {
        DialogData dialogData = getDialogData();
        Status status = StatusService.getStatus(dialogData);
        status.eldsteUlesteForBruker = null;

        statusService.markerSomLestAvBruker(dialogData);

        verify(dialogDAO, only()).oppdaterStatus(status);
    }

    @Test
    public void oppdaterVenterPaNavSidenMedFerdigbehandletSkalSetteVenterPaNavSidenTilNull() {
        DialogData dialogData = getDialogData();
        Status status = StatusService.getStatus(dialogData);
        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, true);
        status.venterPaNavSiden = null;

        statusService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        verify(dialogDAO, only()).oppdaterStatus(status);
    }

    @Test
    public void narJegOppdatererVenterPaNavForventerJegAtVenterPaNavErOppdatert() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(null);
        Status original = StatusService.getStatus(dialogData);
        Date uniktTidspunkt = uniktTidspunkt();
        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        statusService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        verify(dialogDAO, only()).oppdaterStatus(argThat(opppdatert -> verifyKunVenterPaNavEndret(original, uniktTidspunkt, opppdatert)));
    }

    private boolean verifyKunVenterPaNavEndret(Status original, Date uniktTidspunkt, Status opppdatert) {
        original.venterPaNavSiden = opppdatert.venterPaNavSiden;
        return opppdatert.venterPaNavSiden.after(uniktTidspunkt) && opppdatert.equals(original);
    }

    @Test
    public void narJegFjernerVenterPaSvarForventerJegAtVenterPaSvarFraBrukerErNull() {
        DialogData dialogData = getDialogData();
        Status status = StatusService.getStatus(dialogData);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        statusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);

        status.venterPaSvarFraBruker = null;
        verify(dialogDAO, only()).oppdaterStatus(status);
    }


    @Test
    public void narJegVenterPaSvarForventerJegAtVenterPaSvarSettesBlirOppdatert() {
        DialogData dialogData = getDialogData().withVenterPaSvarFraBrukerSiden(null);
        Status original = StatusService.getStatus(dialogData);

        Date uniktTidspunkt = uniktTidspunkt();

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), true, true);
        statusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);

        verify(dialogDAO, only()).oppdaterStatus(argThat(oppdatert -> verifyKunVenterPaSvarFraBrukerFjernet(original, uniktTidspunkt, oppdatert)));
    }

    private boolean verifyKunVenterPaSvarFraBrukerFjernet(Status original, Date uniktTidspunkt, Status oppdatert) {
        original.venterPaSvarFraBruker = oppdatert.venterPaSvarFraBruker;
        return oppdatert.equals(original) && oppdatert.venterPaSvarFraBruker.after(uniktTidspunkt);
    }

    @Test
    public void narJegSetterDialogTilHistoriskForventerJegAtDialogenBlirHistoriskOgAlleFelterBlirNullstilt() {
        DialogData dialogData = getDialogData();

        Status status = new Status(dialogData.getId());
        status.setHistorisk(true);

        statusService.settDialogTilHistorisk(dialogData);
        verify(dialogDAO, only()).oppdaterStatus(status);

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