package no.nav.fo.veilarbdialog.config;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.fo.feed.common.OutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.veilarbdialog.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbdialog.domain.KvpDTO;
import no.nav.fo.veilarbdialog.feed.AvsluttetOppfolgingFeedConsumer;
import no.nav.fo.veilarbdialog.feed.KvpFeedConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Invocation;
import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class FeedConsumerConfig {

    private final LockProvider lockProvider;

    @Value("${application.veilarboppfolging.api.url}")
    private String apiUrl;

    @Bean
    public FeedConsumer<KvpDTO> kvpFeedDTOFeedConsumer(KvpFeedConsumer kvpFeedConsumer, SystemUserTokenProvider provider) {

        no.nav.fo.feed.consumer.FeedConsumerConfig<KvpDTO> config = new no.nav.fo.feed.consumer.FeedConsumerConfig<>(
                new no.nav.fo.feed.consumer.FeedConsumerConfig.BaseConfig<>(
                        KvpDTO.class,
                        kvpFeedConsumer::sisteEndring,
                        apiUrl,
                        KvpDTO.FEED_NAME
                ),
                new no.nav.fo.feed.consumer.FeedConsumerConfig.CronPollingConfig("/10 * * * * ?")
        )
                .lockProvider(lockProvider, 10000)
                .callback(kvpFeedConsumer::lesKvpFeed)
                .interceptors(Collections.singletonList(new SystemUserTokenInterceptor(provider)));

        return new FeedConsumer<>(config);

    }

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer(
            AvsluttetOppfolgingFeedConsumer avsluttetOppfolgingFeedConsumer,
            SystemUserTokenProvider systemUserTokenProvider
    ) {

        no.nav.fo.feed.consumer.FeedConsumerConfig<AvsluttetOppfolgingFeedDTO> config = new no.nav.fo.feed.consumer.FeedConsumerConfig<>(
                new no.nav.fo.feed.consumer.FeedConsumerConfig.BaseConfig<>(
                        AvsluttetOppfolgingFeedDTO.class,
                        avsluttetOppfolgingFeedConsumer::sisteEndring,
                        apiUrl,
                        AvsluttetOppfolgingFeedDTO.FEED_NAME
                ),
                new no.nav.fo.feed.consumer.FeedConsumerConfig.CronPollingConfig("/10 * * * * ?")
        )
                .lockProvider(lockProvider, 10000)
                .callback(avsluttetOppfolgingFeedConsumer::lesAvsluttetOppfolgingFeed)
                .interceptors(Collections.singletonList(new SystemUserTokenInterceptor(systemUserTokenProvider)));

        return new FeedConsumer<>(config);

    }

    @RequiredArgsConstructor
    private static class SystemUserTokenInterceptor implements OutInterceptor {

        private final SystemUserTokenProvider systemUserTokenProvider;

        @Override
        public void apply(Invocation.Builder builder) {
            builder.header("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken());
        }

    }

}
