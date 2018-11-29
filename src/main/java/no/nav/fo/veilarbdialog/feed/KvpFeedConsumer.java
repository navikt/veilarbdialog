package no.nav.fo.veilarbdialog.feed;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import no.nav.fo.veilarbdialog.db.dao.KvpFeedMetadataDAO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

@Component
public class KvpFeedConsumer {

    private final AppService appService;

    private final KvpFeedMetadataDAO kvpFeedConsumerDAO;

    @Inject
    public KvpFeedConsumer(AppService appService,
            KvpFeedMetadataDAO feedConsumerDAO) {
        this.appService = appService;
        this.kvpFeedConsumerDAO = feedConsumerDAO;
    }

    public String sisteEndring() {
        return String.valueOf(kvpFeedConsumerDAO.hentSisteId());
    }

    public void lesKvpFeed(String lastEntryId, List<KvpDTO> elements) {
        long lastSuccessfulId = -1;
        for (KvpDTO element : elements) {
            if(element.getAvsluttetDato() != null) {
                appService.settKontorsperredeDialogerTilHistoriske(element.getAktorId(), element.getAvsluttetDato());
            }
            lastSuccessfulId = element.getSerial();
        }

        // Håndterer ikke exceptions her. Dersom en exception oppstår i løkkeprosesseringen over, vil
        // vi altså IKKE få oppdatert siste id. Dermed vil vi lese feeden på nytt fra siste kjente id og potensielt
        // prosessere noen elementer flere ganger. Dette skal gå bra, siden koden som setter dialoger til historisk
        // er idempotent
        if(lastSuccessfulId > -1) {
            kvpFeedConsumerDAO.oppdaterSisteFeedId(lastSuccessfulId);
        }
    }
}
