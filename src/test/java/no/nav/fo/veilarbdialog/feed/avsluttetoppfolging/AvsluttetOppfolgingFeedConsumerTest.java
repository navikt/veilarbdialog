package no.nav.fo.veilarbdialog.feed.avsluttetoppfolging;

import no.nav.fo.veilarbdialog.db.dao.FeedMetaDataDAO;
import no.nav.fo.veilarbdialog.feed.AvsluttetOppfolgingFeedConsumer;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AvsluttetOppfolgingFeedConsumerTest {

    private FeedMetaDataDAO dao = mock(FeedMetaDataDAO.class);
    private AppService appService = mock(AppService.class);

    @Test(expected = RuntimeException.class)
    public void skalIkkeOppdatereSisteIdHvisException() {
        doThrow(new RuntimeException("Mock exception")).when(appService).settDialogerTilHistoriske(null, null);
        List<AvsluttetOppfolgingFeedDTO> elements = asList(feedElement(new Date(), null, null));

        try {
            new AvsluttetOppfolgingFeedConsumer(appService, dao).lesAvsluttetOppfolgingFeed(null, elements);
        } finally {
            verify(dao, never()).oppdaterSisteLest(any(Date.class));
        }

    }

    private AvsluttetOppfolgingFeedDTO feedElement(Date oppdatertDato, String aktoerId, Date sluttDato) {
        return new AvsluttetOppfolgingFeedDTO().setOppdatert(oppdatertDato).setAktoerid(aktoerId).setSluttdato(sluttDato);
    }

    @Test
    public void skalOppdatereSisteIdHvisOk() {
        Date date1 = new Date();
        Date date2 = new Date(date1.getTime() + 1000);
        List<AvsluttetOppfolgingFeedDTO> elements = asList(feedElement(date1, null, null), feedElement(date2, null, null));

        new AvsluttetOppfolgingFeedConsumer(appService, dao).lesAvsluttetOppfolgingFeed(null, elements);

        verify(dao).oppdaterSisteLest(date2);

    }

    @Test
    public void skalAvslutteForAlleElementerIFeed() {
        String aktor1 = "Aktor1";
        String aktor2 = "Aktor2";
        Date date1 = new Date();
        Date date2 = new Date(date1.getTime() + 1000);
        List<AvsluttetOppfolgingFeedDTO> elements = asList(feedElement(null, aktor1, date1), feedElement(null, aktor2, date2));

        new AvsluttetOppfolgingFeedConsumer(appService, dao).lesAvsluttetOppfolgingFeed(null, elements);

        verify(appService).settDialogerTilHistoriske(aktor1, date1);
        verify(appService).settDialogerTilHistoriske(aktor2, date2);
        verifyNoMoreInteractions(appService);
    }
}
