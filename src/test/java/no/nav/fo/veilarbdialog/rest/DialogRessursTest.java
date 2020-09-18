package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.IntegationTest;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DialogRessursTest extends IntegationTest {

    // TODO: Fix.
    /*private static final String AKTORID = "123";
    private static final String FNR = "4321";

    @Autowired
    private DialogRessurs dialogRessurs;

    @Autowired
    private MockHttpServletRequest mockHttpServletRequest;

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

    static class ContextConfig {

        @Bean
        public AktorService aktorService() {
            AktorService aktorService = mock(AktorService.class);
            when(aktorService.getAktorId(FNR)).thenReturn(Optional.of(AKTORID));
            when(aktorService.getFnr(AKTORID)).thenReturn(Optional.of(FNR));
            return aktorService;
        }

        @Bean
        public PepClient pepClient() {
            return mock(PepClient.class);
        }

        @Bean
        public KvpService kvpClient() {
            return mock(KvpService.class);
        }

        @Bean
        public KafkaDialogService kafkaDialogService() {
            System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");
            return new KafkaDialogService(mock(Producer.class), mock(KafkaDAO.class), mock(DialogDAO.class));
        }

        @Bean
        public UnleashService unleashService() {
            return mock(UnleashService.class);
        }

    }

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(asList(ContextConfig.class,
                DialogDataService.class,
                DialogDAO.class,
                DialogStatusService.class,
                StatusDAO.class,
                DataVarehusDAO.class,
                KladdDAO.class,
                DialogFeedDAO.class,
                Request.class,
                KladdService.class,
                DialogRessurs.class,
                AutorisasjonService.class,
                RestMapper.class,
                KontorsperreFilter.class,
                VarselDAO.class));
    }

    @Component
    public static class Request extends MockHttpServletRequest {
    }

    @Before
    public void setup() {
        subjectRule.setSubject(new Subject("veileder", IdentType.InternBruker, mock(SsoToken.class)));
        mockHttpServletRequest.setParameter("fnr", FNR);
    }

    @Test
    public void opprettOgHentDialoger() throws Exception {
        dialogRessurs.nyHenvendelse(new NyHenvendelseDTO().setTekst("tekst"));
        val hentAktiviteterResponse = dialogRessurs.hentDialoger();
        assertThat(hentAktiviteterResponse, hasSize(1));

        dialogRessurs.markerSomLest(hentAktiviteterResponse.get(0).id);
    }

    @Test
    public void forhandsorienteringPaEksisterendeDialogPaAktivitetSkalFaEgenskapenParagraf8() {
        final String aktivitetId = "123";

        dialogRessurs.nyHenvendelse(
                new NyHenvendelseDTO()
                        .setTekst("forhandsorienteringPaEksisterendeDialogPaAktivitetSkalFaEgenskapenParagraf8")
                        .setAktivitetId(aktivitetId)
        );

        val opprettetDialog = dialogRessurs.hentDialoger();
        assertThat(opprettetDialog.get(0).getEgenskaper().isEmpty(), is(true));
        assertThat(opprettetDialog.size(), is(1));

        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("paragraf8")
                        .setAktivitetId(aktivitetId)
        );

        val dialogMedParagraf8 = dialogRessurs.hentDialoger();
        assertThat(dialogMedParagraf8.get(0).getEgenskaper().contains(Egenskap.PARAGRAF8), is(true));
        assertThat(dialogMedParagraf8.size(), is(1));
    }

    @Test
    public void skalHaParagraf8Egenskap() {
        dialogRessurs.forhandsorienteringPaAktivitet(
                new NyHenvendelseDTO()
                        .setTekst("skalHaParagraf8Egenskap")
                        .setAktivitetId("123")
        );

        val hentedeDialoger = dialogRessurs.hentDialoger();
        assertThat(hentedeDialoger, hasSize(1));
        assertThat(hentedeDialoger.get(0).getEgenskaper().contains(Egenskap.PARAGRAF8), is(true));
    }*/
}
