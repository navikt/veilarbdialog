package no.nav.fo.veilarbdialog.feed.avsluttetkvp;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import no.nav.fo.veilarbdialog.db.dao.KvpFeedMetadataDAO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

public class KvpFeedConsumerTest {

    private KvpFeedMetadataDAO dao = mock(KvpFeedMetadataDAO.class);
    private AppService appService = mock(AppService.class);

    @Test(expected=RuntimeException.class)
    public void skal_ikke_oppdatere_siste_feed_id_hvis_behandling_av_innlesing_feiler() {
        doThrow(new RuntimeException("Mock exception")).when(appService).settKontorsperredeDialogerTilHistoriske(anyString(), any(Date.class));
        List<KvpDTO> elements = asList(feedElement(1, "123", new Date()));

        try {
            new KvpFeedConsumer(appService, dao).lesKvpFeed(null, elements);
        } finally {
            verify(dao, never()).oppdaterSisteFeedId(anyLong());
        }

    }

    @Test
    public void skal_oppdatere_siste_feed_id_hvis_behandling_av_innlesing_er_ok() {
        List<KvpDTO> elements = asList(feedElement(1, null, null), feedElement(2, null, null));

        new KvpFeedConsumer(appService, dao).lesKvpFeed(null, elements);

        verify(dao).oppdaterSisteFeedId(2);

    }

    @Test
    public void skal_sette_dialoger_til_historiske_for_alle_elementer_i_feed_som_har_en_sluttdato() {
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

    @Test
    public void skal_ikke_sette_dialoger_til_historiske_for_elementer_i_feed_som_ikke_har_en_sluttdato() {
        String aktor1 = "Aktor1";
        String aktor2 = "Aktor2";
        Date date1 = mock(Date.class);
        List<KvpDTO> elements = asList(feedElement(1, aktor1, date1), feedElement(2, aktor2, null));

        new KvpFeedConsumer(appService, dao).lesKvpFeed(null, elements);

        verify(appService).settKontorsperredeDialogerTilHistoriske(aktor1, date1);
        verifyNoMoreInteractions(appService);
    }

    private KvpDTO feedElement(long id, String aktoerId, Date sluttDato) {
        return new KvpDTO().setSerial(id).setAktorId(aktoerId).setAvsluttetDato(sluttDato);
    }

}
