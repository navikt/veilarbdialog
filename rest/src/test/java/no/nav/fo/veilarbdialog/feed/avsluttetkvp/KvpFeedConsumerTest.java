package no.nav.fo.veilarbdialog.feed.avsluttetkvp;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import no.nav.fo.veilarbdialog.db.dao.KvpFeedConsumerDAO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

public class KvpFeedConsumerTest {

    private KvpFeedConsumerDAO dao = mock(KvpFeedConsumerDAO.class);
    private AppService appService = mock(AppService.class);        

    @Test(expected=RuntimeException.class)
    public void skalIkkeOppdatereSisteIdHvisException() {
        doThrow(new RuntimeException("Mock exception")).when(appService).settKontorsperredeDialogerTilHistoriske(anyString(), any(Date.class));       
        List<KvpDTO> elements = asList(feedElement(1, "123", new Date()));

        try {
            new KvpFeedConsumer(appService, dao).lesKvpFeed(null, elements);
        } finally {
            verify(dao, never()).oppdaterSisteFeedId(anyLong());
        }
                
    }

    private KvpDTO feedElement(long id, String aktoerId, Date sluttDato) {
        return new KvpDTO().setSerial(id).setAktorId(aktoerId).setAvsluttetDato(sluttDato);
    }
    
    @Test
    public void skalOppdatereSisteIdHvisOk() {
        List<KvpDTO> elements = asList(feedElement(1, null, null), feedElement(2, null, null));

        new KvpFeedConsumer(appService, dao).lesKvpFeed(null, elements);

        verify(dao).oppdaterSisteFeedId(2);
        
    }
    
    @Test
    public void skalAvslutteForAlleElementerIFeed() {
        String aktor1 = "Aktor1";
        String aktor2 = "Aktor2";
        Date date1 = mock(Date.class);
        Date date2 = mock(Date.class);
        List<KvpDTO> elements = asList(feedElement(1, aktor1, date1), feedElement(2, aktor2, date2));

        new KvpFeedConsumer(appService, dao).lesKvpFeed(null, elements);

        verify(appService).settKontorsperredeDialogerTilHistoriske(aktor1, date1);
        verify(appService).settKontorsperredeDialogerTilHistoriske(aktor2, date2);
        verifyNoMoreInteractions(appService);
    }

}
