package no.nav.fo.veilarbdialog.feed;

import no.nav.fo.veilarbdialog.db.dao.FeedConsumerDAO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
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
        Date lastSuccessfulId = null;
        for (AvsluttetOppfolgingFeedDTO element : elements) {
            appService.settDialogerTilHistoriske(element.getAktoerid(), element.getSluttdato());
            lastSuccessfulId = element.getOppdatert();
        }

        // Håndterer ikke exceptions her. Dersom en exception oppstår i løkkeprosesseringen over, vil 
        // vi altså IKKE få oppdatert siste id. Dermed vil vi lese feeden på nytt fra siste kjente id og potensielt
        // prosessere noen elementer flere ganger. Dette skal gå bra, siden koden som setter dialoger til historisk
        // er idempotent
        if(lastSuccessfulId != null) {
            feedConsumerDAO.oppdaterSisteFeedId(lastSuccessfulId);
        }
    }
}
