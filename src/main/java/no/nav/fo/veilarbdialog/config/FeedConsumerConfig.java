package no.nav.fo.veilarbdialog.config;

import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.veilarbdialog.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbdialog.domain.KvpDTO;
import no.nav.fo.veilarbdialog.feed.AvsluttetOppfolgingFeedConsumer;
import no.nav.fo.veilarbdialog.feed.KvpFeedConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.inject.Inject;
import java.util.Collections;

import static no.nav.fo.veilarbdialog.config.ApplicationConfig.VEILARBOPPFOLGINGAPI_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({
        AvsluttetOppfolgingFeedConsumer.class,
        KvpFeedConsumer.class
})
public class FeedConsumerConfig {

    @Inject
    private LockProvider lockProvider;

    @Bean
    public FeedConsumer<KvpDTO> kvpFeedDTOFeedConsumer(KvpFeedConsumer kvpFeedConsumer) {
        no.nav.fo.feed.consumer.FeedConsumerConfig<KvpDTO> config = new no.nav.fo.feed.consumer.FeedConsumerConfig<>(
                new no.nav.fo.feed.consumer.FeedConsumerConfig.BaseConfig<>(
                        KvpDTO.class,
                        kvpFeedConsumer::sisteEndring,
                        getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY),
                        KvpDTO.FEED_NAME
                ),
                new no.nav.fo.feed.consumer.FeedConsumerConfig.CronPollingConfig("/10 * * * * ?")
        )
                .lockProvider(lockProvider, 10000)
                .callback(kvpFeedConsumer::lesKvpFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer(
            AvsluttetOppfolgingFeedConsumer avsluttetOppfolgingFeedConsumer) {
        no.nav.fo.feed.consumer.FeedConsumerConfig<AvsluttetOppfolgingFeedDTO> config = new no.nav.fo.feed.consumer.FeedConsumerConfig<>(
                new no.nav.fo.feed.consumer.FeedConsumerConfig.BaseConfig<>(
                        AvsluttetOppfolgingFeedDTO.class,
                        avsluttetOppfolgingFeedConsumer::sisteEndring,
                        getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY),
                        AvsluttetOppfolgingFeedDTO.FEED_NAME
                ),
                new no.nav.fo.feed.consumer.FeedConsumerConfig.CronPollingConfig("/10 * * * * ?")
        )
                .lockProvider(lockProvider, 10000)
                .callback(avsluttetOppfolgingFeedConsumer::lesAvsluttetOppfolgingFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

}
