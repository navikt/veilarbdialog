package no.nav.fo.veilarbdialog.feed;

import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.db.dao.FeedConsumerDAO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Component
public class AvsluttetOppfolgingFeedConsumer {

    private final AppService appService;

    private final FeedConsumerDAO feedConsumerDAO;

    @Inject
    public AvsluttetOppfolgingFeedConsumer(AppService appService,
                                           FeedConsumerDAO feedConsumerDAO) {
        this.appService = appService;
        this.feedConsumerDAO = feedConsumerDAO;
    }

    String sisteEndring() {
        Date sisteEndring = feedConsumerDAO.hentSisteHistoriskeTidspunkt();
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }

    void lesAvsluttetOppfolgingFeed(String lastEntryId, List<AvsluttetOppfolgingFeedDTO> elements) {
        elements.forEach(appService::settDialogerTilHistoriske);
    }
}
