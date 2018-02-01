package no.nav.fo.veilarbdialog.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.of;
import static no.nav.apiapp.util.StringUtils.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppServiceTest {

    private static final long DIALOG_ID = 42;
    private static final String AKTOR_ID = "aktorId";
    private static final String IDENT = "ident";
    private static final String AKTIVITET_ID = "aktivitetId";
    private static final DialogStatus DIALOG_STATUS = DialogStatus.builder().dialogId(DIALOG_ID).build();
    private static final HenvendelseData NY_HENVENDELSE = HenvendelseData.builder().aktivitetId(AKTIVITET_ID).dialogId(DIALOG_ID).build();
    private static final DialogData DIALOG_DATA = DialogData.builder().id(DIALOG_ID).aktorId(AKTOR_ID).build();

    private final DialogDAO dialogDAO = mock(DialogDAO.class);
    private final DialogFeedDAO dialogFeedDAO = mock(DialogFeedDAO.class);
    private final AktorService aktorService = mock(AktorService.class);
    private final PepClient pepClient = mock(PepClient.class);
    private final KvpClient kvpClient = mock(KvpClient.class);
    private AppService appService = new AppService(
            aktorService,
            dialogDAO,
            dialogFeedDAO,
            pepClient,
            kvpClient);
    private Answer<?> ingenAbacTilgang = (a) -> {
        throw new IngenTilgang();
    };
    private Answer<?> abacTilgang = (a) -> a.getArgument(0);

    @Before
    public void setup() {
        mockDialog(DIALOG_DATA);
        when(aktorService.getFnr(AKTOR_ID)).thenReturn(of(IDENT));
        when(aktorService.getAktorId(IDENT)).thenReturn(of(AKTOR_ID));
    }

    private void mockDialog(DialogData dialogData) {
        when(dialogDAO.opprettDialog(DIALOG_DATA)).thenReturn(DIALOG_ID);
        when(dialogDAO.hentDialog(DIALOG_ID)).thenReturn(dialogData);
        when(dialogDAO.hentDialogForAktivitetId(AKTIVITET_ID)).thenReturn(of(DIALOG_DATA));
    }

    @Test
    public void tilgangskontroll__ingen_tilgang() {
        mockAbac(ingenAbacTilgang);
        sjekkIngenTilgang(
                IngenTilgang.class,
                this::hentDialog,
                this::hentDialogerForBruker,
                this::oppdaterFerdigbehandletTidspunkt,
                this::oppdaterVentePaSvarTidspunkt,
                this::opprettHenvendelseForDialog,
                this::markerDialogSomLestAvBruker,
                this::markerDialogSomLestAvVeileder,
                this::opprettDialogForAktivitetsplanPaIdent,
                this::hentDialogForAktivitetId
        );
    }

    @Test
    public void tilgangskontroll__lesetilgang() {
        mockAbac(abacTilgang);
        mockDialog(DIALOG_DATA.withHistorisk(true));

        sjekkTilgang(
                this::hentDialog,
                this::hentDialogerForBruker,
                this::hentAktoerIdForIdent,
                this::markerDialogSomLestAvBruker,
                this::markerDialogSomLestAvVeileder,
                this::hentDialogForAktivitetId,
                this::opprettDialogForAktivitetsplanPaIdent
        );
        sjekkIngenTilgang(
                UlovligHandling.class,
                this::oppdaterFerdigbehandletTidspunkt,
                this::oppdaterVentePaSvarTidspunkt,
                this::opprettHenvendelseForDialog
        );
    }

    @Test
    public void tilgangskontroll__skrivetilgang() {
        mockAbac(abacTilgang);
        sjekkTilgang(
                this::hentDialog,
                this::hentDialogerForBruker,
                this::hentAktoerIdForIdent,
                this::oppdaterFerdigbehandletTidspunkt,
                this::oppdaterVentePaSvarTidspunkt,
                this::opprettHenvendelseForDialog,
                this::markerDialogSomLestAvBruker,
                this::markerDialogSomLestAvVeileder,
                this::opprettDialogForAktivitetsplanPaIdent,
                this::hentDialogForAktivitetId
        );
    }

    private void mockAbac(Answer<?> okAnswer) {
        when(pepClient.sjekkLeseTilgangTilFnr(IDENT)).thenAnswer(okAnswer);
    }

    private void sjekkIngenTilgang(Class<? extends Exception> exceptionClass, Runnable... runnable) {
        Arrays.asList(runnable).forEach(r -> assertThatThrownBy(r::run)
                .isInstanceOf(exceptionClass)
                .describedAs(r.toString())
        );
    }

    private void sjekkTilgang(Runnable... runnable) {
        Arrays.asList(runnable).forEach(Runnable::run);
    }

    private void hentDialog() {
        appService.hentDialog(DIALOG_ID);
    }

    private Optional<DialogData> hentDialogForAktivitetId() {
        return appService.hentDialogForAktivitetId(AKTIVITET_ID);
    }

    private DialogData opprettDialogForAktivitetsplanPaIdent() {
        return appService.opprettDialogForAktivitetsplanPaIdent(DIALOG_DATA);
    }

    private DialogData markerDialogSomLestAvVeileder() {
        return appService.markerDialogSomLestAvVeileder(DIALOG_ID);
    }

    private DialogData markerDialogSomLestAvBruker() {
        return appService.markerDialogSomLestAvBruker(DIALOG_ID);
    }

    private DialogData opprettHenvendelseForDialog() {
        return appService.opprettHenvendelseForDialog(NY_HENVENDELSE);
    }

    private DialogData oppdaterVentePaSvarTidspunkt() {
        return appService.oppdaterVentePaSvarTidspunkt(DIALOG_STATUS);
    }

    private DialogData oppdaterFerdigbehandletTidspunkt() {
        return appService.oppdaterFerdigbehandletTidspunkt(DIALOG_STATUS);
    }

    private String hentAktoerIdForIdent() {
        return appService.hentAktoerIdForIdent(IDENT);
    }

    private List<DialogData> hentDialogerForBruker() {
        return appService.hentDialogerForBruker(IDENT);
    }

}