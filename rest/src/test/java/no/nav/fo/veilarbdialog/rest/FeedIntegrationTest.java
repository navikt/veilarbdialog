package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.feed.FeedProducerTester;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Date;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;

public class FeedIntegrationTest {

    @Nested
    public class henvendelser extends Base {

        @Override
        public void opprettElementForFeed(String feedName, String id) {
            setCurrentTimestamp(0);
            val dialogId = dialogDAO.opprettDialog(nyDialog()
                    .withAktorId(id)
            );

            setCurrentTimestamp(DateUtils.toDate(id).getTime());
            dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, id, BRUKER));
        }

    }

    @Nested
    public class statusOppdateringer extends Base {

        @Override
        public void opprettElementForFeed(String feedName, String id) {
            setCurrentTimestamp(DateUtils.toDate(id).getTime());
            dialogDAO.opprettDialog(nyDialog()
                    .withAktorId(id)
            );
        }

    }

    public static abstract class Base extends IntegrasjonsTest implements FeedProducerTester {

        private static long counter = 1;

        @Inject
        protected FeedController feedController;

        @Inject
        protected DialogDAO dialogDAO;

        @BeforeEach
        void setup() {
            System.setProperty("dialogaktor.feed.brukertilgang", SubjectHandler.getSubjectHandler().getUid());
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
            return ISO8601FromDate(new Date(counter++));
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