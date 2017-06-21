package no.nav.fo.veilarbdialog.feed;

import no.nav.fo.veilarbdialog.db.dao.DialogFeedDAO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Component
public class AvsluttetOppfolgingFeedProvider {

    @Inject
    DialogFeedDAO dialogFeedDAO;

    @Inject
    AppService appService;

    public String sisteEndring() {
        Date sisteEndring = dialogFeedDAO.hentSisteHistoriskeTidspunkt();
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }

    public void lesAvsluttetOppfolgingFeed(String lastEntryId, List<AvsluttetOppfolgingFeedDTO> elements) {
        elements.forEach(element -> appService.settDialogerTilHistoriske(element));
    }
}
