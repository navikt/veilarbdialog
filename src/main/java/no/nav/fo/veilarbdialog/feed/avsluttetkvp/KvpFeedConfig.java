package no.nav.fo.veilarbdialog.feed.avsluttetkvp;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import no.nav.sbl.util.EnvironmentUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Collections;

import static no.nav.fo.veilarbdialog.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;

@Configuration
public class KvpFeedConfig {

    private String host = EnvironmentUtils.getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY);

    @Value("${kvp.feed.consumer.pollingrate:/10 * * * * ?}")
    private String polling;

    @Inject
    private DataSource dataSource;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<KvpDTO> kvpFeedDTOFeedConsumer(KvpFeedConsumer kvpFeedConsumer) {
        FeedConsumerConfig<KvpDTO> config = new FeedConsumerConfig<>(
                new FeedConsumerConfig.BaseConfig<>(
                        KvpDTO.class,
                        kvpFeedConsumer::sisteEndring,
                        host,
                        KvpDTO.FEED_NAME
                ),
                new FeedConsumerConfig.CronPollingConfig(polling)
        )
                .lockProvider(lockProvider(dataSource), 10000)
                .callback(kvpFeedConsumer::lesKvpFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }
}
