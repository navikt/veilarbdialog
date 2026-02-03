package no.nav.fo.veilarbdialog.config;

import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.oidc.filter.AzureAdUserRoleResolver;
import no.nav.common.auth.oidc.filter.OidcAuthenticationFilter;
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig;
import no.nav.common.rest.filter.LogRequestFilter;
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter;
import no.nav.common.token_client.utils.env.TokenXEnvironmentvariables;
import no.nav.fo.veilarbdialog.util.PingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static no.nav.common.auth.oidc.filter.OidcAuthenticator.fromConfigs;
import static no.nav.fo.veilarbdialog.rest.AdminController.PTO_ADMIN_SERVICE_USER;

@Configuration
public class FilterConfig {

    private static final List<String> ALLOWED_SERVICE_USERS = List.of(
            "srvveilarbportefolje", "srvveilarbdirigent", "srvveilarboppfolging", PTO_ADMIN_SERVICE_USER
    );

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.cluster}")
    private String applicationCluster;

    @Value("${application.azure.ad.discoveryUrl}")
    private String azureAdDiscoveryUrl;

    @Value("${application.azure.ad.clientId}")
    private String azureAdClientId;

    private OidcAuthenticatorConfig naisAzureAdConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(azureAdDiscoveryUrl)
                .withClientId(azureAdClientId)
                .withUserRoleResolver(new AzureAdUserRoleResolver());
    }

    private OidcAuthenticatorConfig tokenxConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(System.getenv(TokenXEnvironmentvariables.TOKEN_X_WELL_KNOWN_URL))
                .withClientId(System.getenv(TokenXEnvironmentvariables.TOKEN_X_CLIENT_ID))
                .withUserRole(UserRole.EKSTERN);
    }

    @Bean
    public FilterRegistrationBean<PingFilter> pingFilter() {
        // Veilarbproxy trenger dette endepunktet for å sjekke at tjenesten lever
        // /internal kan ikke brukes siden det blir stoppet før det kommer frem

        FilterRegistrationBean<PingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PingFilter());
        registration.setOrder(1);
        registration.addUrlPatterns("/api/ping");
        return registration;
    }


    @Bean
    public FilterRegistrationBean<LogRequestFilter> logFilterRegistrationBean() {
        boolean dev = applicationCluster.contains("dev");
        FilterRegistrationBean<LogRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LogRequestFilter(applicationName, dev));
        registration.setOrder(2);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TeamLogsRequestLoggerFilter> teamLogsRequestLoggerFilterRegistrationBean(TeamLogsRequestLoggerFilter teamLogsRequestLoggerFilter) {
        FilterRegistrationBean<TeamLogsRequestLoggerFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(teamLogsRequestLoggerFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(3);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(
            value = "application.oidc.disabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public FilterRegistrationBean<OidcAuthenticationFilter> authenticationFilterRegistrationBean() {
        FilterRegistrationBean<OidcAuthenticationFilter> registration = new FilterRegistrationBean<>();
        var authenticationFilter = new OidcAuthenticationFilter(
                fromConfigs(
                        naisAzureAdConfig(),
                        tokenxConfig()
                )
        );
        registration.setFilter(authenticationFilter);
        registration.setOrder(4);
        registration.addUrlPatterns("/api/*");
        registration.addUrlPatterns("/graphql");
        registration.addUrlPatterns(("/internal/api/*"));
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SetStandardHttpHeadersFilter> setStandardHeadersFilterRegistrationBean() {
        FilterRegistrationBean<SetStandardHttpHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetStandardHttpHeadersFilter());
        registration.setOrder(6);
        registration.addUrlPatterns("/*");
        return registration;
    }

}
