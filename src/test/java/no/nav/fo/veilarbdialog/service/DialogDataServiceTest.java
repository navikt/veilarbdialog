package no.nav.fo.veilarbdialog.service;

public class DialogDataServiceTest {

    // TODO: Fix.
    /*private static final long DIALOG_ID = 42;
    private static final String AKTOR_ID = "aktorId";
    private static final String IDENT = "ident";
    private static final String AKTIVITET_ID = "aktivitetId";
    private static final String KONTORSPERRE_ENHET_ID = "1337";
    private static final DialogStatus DIALOG_STATUS = DialogStatus.builder().dialogId(DIALOG_ID).build();
    private static final HenvendelseData NY_HENVENDELSE = HenvendelseData.builder().aktivitetId(AKTIVITET_ID).dialogId(DIALOG_ID).build();
    private static final DialogData DIALOG_DATA = DialogData.builder().id(DIALOG_ID).aktorId(AKTOR_ID).build();

    private final DialogDAO dialogDAO = mock(DialogDAO.class);
    private final DialogStatusService dialogStatusService = mock(DialogStatusService.class);
    private final DialogFeedDAO dialogFeedDAO = mock(DialogFeedDAO.class);
    private final DataVarehusDAO dataVarehusDAO = mock(DataVarehusDAO.class);
    private final AktorService aktorService = mock(AktorService.class);
    private final PepClient pepClient = mock(PepClient.class);
    private final KvpService kvpService = mock(KvpService.class);
    private final UnleashService unleashService = mock(UnleashService.class);

    private DialogDataService dialogDataService;


    @Before
    public void setup() {

        System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");
        KafkaDialogService kafkaDialogService = mock(KafkaDialogService.class);
        this.dialogDataService = new DialogDataService(
                aktorService,
                dialogDAO,
                dialogStatusService,
                dataVarehusDAO,
                dialogFeedDAO,
                pepClient,
                kafkaDialogService,
                kvpService,
                unleashService
        );

        mockDialog(DIALOG_DATA);
        when(aktorService.getFnr(AKTOR_ID)).thenReturn(of(IDENT));
        when(aktorService.getAktorId(IDENT)).thenReturn(of(AKTOR_ID));
    }

    private void mockDialog(DialogData dialogData) {
        when(dialogDAO.opprettDialog(DIALOG_DATA)).thenReturn(dialogData);
        when(dialogDAO.hentDialog(DIALOG_ID)).thenReturn(dialogData);
        when(dialogDAO.hentDialogForAktivitetId(AKTIVITET_ID)).thenReturn(of(DIALOG_DATA));
    }

    @Test
    public void kontorsperre_tagger_dialog_med_enhet_id() {
        when(kvpService.kontorsperreEnhetId(AKTOR_ID)).thenReturn(KONTORSPERRE_ENHET_ID);
        dialogDataService.opprettDialogForAktivitetsplanPaIdent(DIALOG_DATA);
        verify(dialogDAO, times(1)).opprettDialog(DIALOG_DATA.withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID));
    }

    @Test
    public void kontorsperre_tagger_henvendelse_med_enhet_id() {
        when(kvpService.kontorsperreEnhetId(AKTOR_ID)).thenReturn(KONTORSPERRE_ENHET_ID);
        dialogDataService.opprettHenvendelseForDialog(NY_HENVENDELSE);
        verify(dialogDAO, times(1)).opprettHenvendelse(NY_HENVENDELSE.withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID));
    }

    @Test
    public void kontorsperre_tagger_dialog_med_null() {
        when(kvpService.kontorsperreEnhetId(AKTOR_ID)).thenReturn(null);
        dialogDataService.opprettDialogForAktivitetsplanPaIdent(DIALOG_DATA);
        verify(dialogDAO, times(1)).opprettDialog(DIALOG_DATA);
    }

    @Test
    public void kontorsperre_tagger_henvendelse_med_null() {
        when(kvpService.kontorsperreEnhetId(AKTOR_ID)).thenReturn(null);
        dialogDataService.opprettHenvendelseForDialog(NY_HENVENDELSE);
        verify(dialogDAO, times(1)).opprettHenvendelse(NY_HENVENDELSE);
    }

    @Test
    public void tilgangskontroll__ingen_tilgang() {
        mockAbacIngenTilgang();
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
        mockAbacTilgang();
        mockDialog(DIALOG_DATA.withHistorisk(true));

        sjekkTilgang(
                this::hentDialog,
                this::hentDialogerForBruker,
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
        mockAbacTilgang();
        sjekkTilgang(
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

    private void mockAbacIngenTilgang() {
        doThrow(new IngenTilgang()).when(pepClient).sjekkLesetilgangTilAktorId((any()));
    }

    private void mockAbacTilgang() {
        reset(pepClient);
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
        dialogDataService.hentDialog(DIALOG_ID);
    }

    private Optional<DialogData> hentDialogForAktivitetId() {
        return dialogDataService.hentDialogForAktivitetId(AKTIVITET_ID);
    }

    private DialogData opprettDialogForAktivitetsplanPaIdent() {
        return dialogDataService.opprettDialogForAktivitetsplanPaIdent(DIALOG_DATA);
    }

    private DialogData markerDialogSomLestAvVeileder() {
        return dialogDataService.markerDialogSomLestAvVeileder(DIALOG_ID);
    }

    private DialogData markerDialogSomLestAvBruker() {
        return dialogDataService.markerDialogSomLestAvBruker(DIALOG_ID);
    }

    private DialogData opprettHenvendelseForDialog() {
        return dialogDataService.opprettHenvendelseForDialog(NY_HENVENDELSE);
    }

    private DialogData oppdaterVentePaSvarTidspunkt() {
        return dialogDataService.oppdaterVentePaSvarTidspunkt(DIALOG_STATUS);
    }

    private DialogData oppdaterFerdigbehandletTidspunkt() {
        return dialogDataService.oppdaterFerdigbehandletTidspunkt(DIALOG_STATUS);
    }

    private List<DialogData> hentDialogerForBruker() {
        return dialogDataService.hentDialogerForBruker(Person.fnr(IDENT));
    }
*/
}
