package no.nav.fo.veilarbdialog.config;

import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.veilarbdialog.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbdialog.domain.KvpDTO;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class FeedConsumerConfig {

    @MockBean
    private FeedConsumer<KvpDTO> kvpDTOFeedConsumer;

    @MockBean
    private FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer;

    @Bean
    public FeedConsumer<KvpDTO> kvpFeedDTOFeedConsumer() {
        return kvpDTOFeedConsumer;
    }

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer() {
        return avsluttetOppfolgingFeedDTOFeedConsumer;
    }

}
