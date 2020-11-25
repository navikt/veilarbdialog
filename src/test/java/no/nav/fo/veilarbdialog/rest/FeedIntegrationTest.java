package no.nav.fo.veilarbdialog.rest;

public class FeedIntegrationTest {

    // TODO: Fix.
    /*private static final String TEST_IDENT = FeedIntegrationTest.class.getSimpleName();

    @Nested
    public class henvendelser extends Base {
        @Override
        public void opprettElementForFeed(String feedName, String id) {
            setCurrentTimestamp(DateUtils.toDate(id).getTime());
            DialogData dialogData = dialogDAO.opprettDialog(nyDialog()
                    .withAktorId("aktorId")
            );

            dialogDAO.opprettHenvendelse(nyHenvendelse(dialogData.getId(), "aktorId", BRUKER).withSendt(new Date()));

            List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId("aktorId");
            dialogFeedDAO.updateDialogAktorFor("aktorId", dialoger);
        }

    }

    @Nested
    public class statusOppdateringer extends Base {

        @Override
        public void opprettElementForFeed(String feedName, String id) {
            setCurrentTimestamp(DateUtils.toDate(id).getTime());
            dialogDAO.opprettDialog(nyDialog()
                    .withAktorId("aktorId")
            );
            List<DialogData> dialoger = dialogDAO.hentDialogerForAktorId("aktorId");
            dialogFeedDAO.updateDialogAktorFor("aktorId", dialoger);
        }
    }

    @ExtendWith(SubjectExtension.class)
    public static abstract class Base extends IntegationTest implements FeedProducerTester {

        private static long counter = 1;

        @Autowired
        protected FeedController feedController;

        @Autowired
        protected DialogDAO dialogDAO;

        @Autowired
        protected DialogFeedDAO dialogFeedDAO;

        static class ContextConfig {

            @Bean
            public AktorService aktorService() {
                return mock(AktorService.class);
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
            public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer() {
                return mock(FeedConsumer.class);
            }

            @Bean
            public FeedConsumer<KvpDTO> kvpDTOFeedConsumer() {
                return mock(FeedConsumer.class);
            }


            @Bean
            public KafkaDialogService kafkaDialogService() {
                System.setProperty("APP_ENVIRONMENT_NAME", "TEST-Q0");
                return new KafkaDialogService(mock(Producer.class), mock(KafkaDAO.class), mock(DialogDAO.class));
            }

            @Bean
            public UnleashService unleashService() {
                return new UnleashServiceMock(false);
            }

        }

        @BeforeAll
        public static void addSpringBeans() {
            initSpringContext(Arrays.asList(ContextConfig.class,
                    DialogDataService.class,
                    DialogDAO.class,
                    DialogStatusService.class,
                    StatusDAO.class,
                    DataVarehusDAO.class,
                    DialogFeedDAO.class,
                    FeedConfig.class,
                    VarselDAO.class));
        }

        @BeforeEach
        void setup(SubjectExtension.SubjectStore subjectStore) {
            subjectStore.setSubject(new Subject(TEST_IDENT, InternBruker, oidcToken("token")));
            System.setProperty(DIALOGAKTOR_FEED_BRUKERTILGANG_PROPERTY, TEST_IDENT);
        }

        protected void setCurrentTimestamp(long time) {
            changeDateProvider(() -> "'" + new Timestamp(time) + "'");
        }

        @Override
        public FeedController getFeedController() {
            return feedController;
        }

        @Override
        public String unikId(String feedName) {
            return ISO8601FromDate(new Date(counter += 2));
        }

        @Override
        public String forsteMuligeId(String feedName) {
            return ISO8601FromDate(new Date(0));
        }

        private String ISO8601FromDate(Date date) {
            return DateUtils.ISO8601FromDate(date, ZoneId.systemDefault());
        }

    }*/
}
