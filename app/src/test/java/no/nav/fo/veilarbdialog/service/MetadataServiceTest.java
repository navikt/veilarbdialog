package no.nav.fo.veilarbdialog.service;

import lombok.SneakyThrows;
import no.nav.fo.veilarbdialog.TestDataBuilder;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.UtilDAO;
import no.nav.fo.veilarbdialog.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.*;

class MetadataServiceTest {

    private DialogDAO dialogDAO = mock(DialogDAO.class);
    private UtilDAO utilDAO = mock(UtilDAO.class);
    private MetadataService metadataService = new MetadataService(dialogDAO, utilDAO);

    @Test
    public void nyHenvendelseFraBrukerEndrerEldsteUlesteForVeilederOgVenterPaNav() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        when(utilDAO.getTimestampFromDB()).thenReturn(uniktTidspunkt);

        Status original = MetadataService.getStatus(dialogData);
        original.settVenterPaNavSiden(uniktTidspunkt);
        original.setUlesteMeldingerForVeileder(uniktTidspunkt);

        metadataService.nyHenvendelse(dialogData, henvendelseData);
        verify(dialogDAO, only()).oppdaterStatus(original);
    }

    @Test
    public void nyHenvendelseFraVeilederEndrerEldsteUlesteForBruker() {
        DialogData dialogData = TestDataBuilder.nyDialog();
        Date uniktTidspunkt = new Date();
        HenvendelseData henvendelseData = nyHenvendelseFraVeileder(dialogData, uniktTidspunkt);

        Status status = MetadataService.getStatus(dialogData);
        status.eldsteUlesteForBruker = uniktTidspunkt;

        metadataService.nyHenvendelse(dialogData, henvendelseData);
        verify(dialogDAO).oppdaterStatus(status);
    }

    @Test
    public void nyHenvendelsePaEksisterendeDialogSkalKalleOppdatertStatusMedSammeStatus() {
        DialogData dialogData = getDialogData();
        Date uniktTidspunkt = uniktTidspunkt();
        HenvendelseData henvendelseData = nyHenvendelseFraBruker(dialogData, uniktTidspunkt);

        Status status = MetadataService.getStatus(dialogData);

        metadataService.nyHenvendelse(dialogData, henvendelseData);
        verify(dialogDAO).oppdaterStatus(status);
    }

    @Test
    public void markerSomLestAvVeilederSkalSetteEldsteUlesteForVeilederTilNull() {
        DialogData dialogData = getDialogData();
        Status status = MetadataService.getStatus(dialogData);
        status.eldsteUlesteForVeileder = null;

        metadataService.markerSomLestAvVeileder(dialogData);

        verify(dialogDAO, Mockito.only()).oppdaterStatus(status);
    }

    @Test
    public void markerSomLestAvBrukerSkalSetteEldsteUlesteForBrukerTilNull() {
        DialogData dialogData = getDialogData();
        Status status = MetadataService.getStatus(dialogData);
        status.eldsteUlesteForBruker = null;

        metadataService.markerSomLestAvBruker(dialogData);

        verify(dialogDAO, only()).oppdaterStatus(status);
    }

    @Test
    public void oppdaterVenterPaNavSidenMedFerdigbehandletSkalSetteVenterPaNavSidenTilNull() {
        DialogData dialogData = getDialogData();
        Status status = MetadataService.getStatus(dialogData);
        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, true);
        status.venterPaNavSiden = null;

        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        verify(dialogDAO, only()).oppdaterStatus(status);
    }

    @Test
    public void narJegSetterVenterPaNavForventerJegAtVenterPaNavBlirSatt() {
        DialogData dialogData = getDialogData().withVenterPaNavSiden(null);
        Status original = MetadataService.getStatus(dialogData);
        Date uniktTidspunkt = uniktTidspunkt();
        original.settVenterPaNavSiden(uniktTidspunkt);

        when(utilDAO.getTimestampFromDB()).thenReturn(uniktTidspunkt);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaNavSiden(dialogData, dialogStatus);

        verify(dialogDAO).oppdaterStatus(original);
    }

    private boolean verifyKunVenterPaNavEndret(Status original, Date uniktTidspunkt, Status opppdatert) {
        original.venterPaNavSiden = opppdatert.venterPaNavSiden;
        return opppdatert.venterPaNavSiden.after(uniktTidspunkt) && opppdatert.equals(original);
    }

    @Test
    public void narJegFjernerVenterPaSvarForventerJegAtVenterPaSvarFraBrukerErNull() {
        DialogData dialogData = getDialogData();
        Status status = MetadataService.getStatus(dialogData);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), false, false);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);

        status.venterPaSvarFraBruker = null;
        verify(dialogDAO, only()).oppdaterStatus(status);
    }


    @Test
    public void narJegVenterPaSvarForventerJegAtVenterPaSvarSettesBlirOppdatert() {
        DialogData dialogData = getDialogData().withVenterPaSvarFraBrukerSiden(null);
        Status original = MetadataService.getStatus(dialogData);

        Date uniktTidspunkt = uniktTidspunkt();
        original.settVenterPaSvarFraBruker(uniktTidspunkt);

        when(utilDAO.getTimestampFromDB()).thenReturn(uniktTidspunkt);

        DialogStatus dialogStatus = new DialogStatus(dialogData.getId(), true, true);
        metadataService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatus);

        verify(dialogDAO).oppdaterStatus(original);
    }

    @Test
    public void narJegSetterDialogTilHistoriskForventerJegAtDialogenBlirHistoriskOgAlleFelterBlirNullstilt() {
        DialogData dialogData = getDialogData();

        Status status = new Status(dialogData.getId());
        status.setHistorisk(true);

        metadataService.settDialogTilHistorisk(dialogData);
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