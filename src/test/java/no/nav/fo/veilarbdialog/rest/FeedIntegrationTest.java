package no.nav.fo.veilarbdialog.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectExtension;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.DbTest;
import no.nav.fo.feed.FeedProducerTester;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.veilarbdialog.client.KvpClient;
import no.nav.fo.veilarbdialog.db.dao.*;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.config.FeedConfig;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.service.DialogStatusService;
import no.nav.fo.veilarbdialog.util.DateUtils;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.common.auth.SsoToken.oidcToken;
import static no.nav.fo.veilarbdialog.config.ApplicationContext.DIALOGAKTOR_FEED_BRUKERTILGANG_PROPERTY;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static org.mockito.Mockito.mock;

public class FeedIntegrationTest {


    private static final String TEST_IDENT = FeedIntegrationTest.class.getSimpleName();

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
    public static abstract class Base extends DbTest implements FeedProducerTester {

        private static long counter = 1;

        @Inject
        protected FeedController feedController;

        @Inject
        protected DialogDAO dialogDAO;

        @Inject
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
            public KvpClient kvpClient() {
                return mock(KvpClient.class);
            }

            @Bean
            public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer() {
                return mock(FeedConsumer.class);
            }

            @Bean
            public FeedConsumer<KvpDTO> kvpDTOFeedConsumer() {
                return mock(FeedConsumer.class);
            }

        }

        @BeforeAll
        public static void addSpringBeans() {
            initSpringContext(Arrays.asList(ContextConfig.class,
                    AppService.class,
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

    }
}
