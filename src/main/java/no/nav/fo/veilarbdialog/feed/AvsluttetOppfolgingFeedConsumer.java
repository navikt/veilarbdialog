package no.nav.fo.veilarbdialog.feed;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.db.dao.FeedMetaDataDAO;
import no.nav.fo.veilarbdialog.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Component
@RequiredArgsConstructor
public class AvsluttetOppfolgingFeedConsumer {

    private final DialogDataService service;
    private final FeedMetaDataDAO feedMetaDataDAO;

    public String sisteEndring() {
        ZonedDateTime sisteEndring = feedMetaDataDAO.hentSisteLestTidspunkt();
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }

    public void lesAvsluttetOppfolgingFeed(String lastEntryId, List<AvsluttetOppfolgingFeedDTO> elements) {
        Date lastSuccessfulId = null;
        for (AvsluttetOppfolgingFeedDTO element : elements) {
            service.settDialogerTilHistoriske(element.getAktoerid(), element.getSluttdato());
            lastSuccessfulId = element.getOppdatert();
        }

        // Håndterer ikke exceptions her. Dersom en exception oppstår i løkkeprosesseringen over, vil
        // vi altså IKKE få oppdatert siste id. Dermed vil vi lese feeden på nytt fra siste kjente id og potensielt
        // prosessere noen elementer flere ganger. Dette skal gå bra, siden koden som setter dialoger til historisk
        // er idempotent
        if (lastSuccessfulId != null) {
            feedMetaDataDAO.oppdaterSisteLest(lastSuccessfulId);
        }
    }
}
