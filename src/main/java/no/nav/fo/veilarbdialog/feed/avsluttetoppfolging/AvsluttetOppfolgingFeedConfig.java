package no.nav.fo.veilarbdialog.feed.avsluttetoppfolging;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.consumer.FeedConsumerConfig.BaseConfig;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Collections;

import static no.nav.fo.veilarbdialog.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class AvsluttetOppfolgingFeedConfig {

    private String host = getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY);

    @Value("${avsluttoppfolging.feed.consumer.pollingrate:/10 * * * * ?}")
    private String polling;

    @Inject
    private DataSource dataSource;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer(
            AvsluttetOppfolgingFeedConsumer avsluttetOppfolgingFeedConsumer) {
        BaseConfig<AvsluttetOppfolgingFeedDTO> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                AvsluttetOppfolgingFeedDTO.class,
                avsluttetOppfolgingFeedConsumer::sisteEndring,
                host,
                AvsluttetOppfolgingFeedDTO.FEED_NAME
        );
        FeedConsumerConfig<AvsluttetOppfolgingFeedDTO> config = new FeedConsumerConfig<>(
                baseConfig,
                new FeedConsumerConfig.CronPollingConfig(polling)
        )
                .lockProvider(lockProvider(dataSource), 10000)
                .callback(avsluttetOppfolgingFeedConsumer::lesAvsluttetOppfolgingFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }
}
