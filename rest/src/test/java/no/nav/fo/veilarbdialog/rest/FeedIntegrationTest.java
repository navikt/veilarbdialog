package no.nav.fo.veilarbdialog.rest;

import lombok.val;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.feed.FeedProducerTester;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class FeedIntegrationTest {

    @Nested
    public class henvendelser extends Base {

        @Override
        public void kanHenteElementerFraAlleProdusenter() {
            String tilfeldigId = unikId(null);
            opprettElementForFeed(null, tilfeldigId);
            List<? extends FeedElement<?>> elements = getFeedController().get(null, forsteMuligeId(null), null).getElements();
            assertThat(elements).hasSize(2);
            assertThat(elements.get(0).getId()).isEqualTo(tilfeldigId);
            assertThat(elements.get(1).getId()).isEqualTo(tilfeldigId);
        }

        @Override
        public void opprettElementForFeed(String feedName, String aktorId) {
            setCurrentTimestamp(DateUtils.toDate(aktorId).getTime());
            val dialogId = dialogDAO.opprettDialog(nyDialog()
                    .withAktorId(aktorId)
            );

            setCurrentTimestamp(DateUtils.toDate(aktorId).getTime() + 1);
            dialogDAO.opprettHenvendelse(aktorId, nyHenvendelse(dialogId, aktorId, BRUKER));
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