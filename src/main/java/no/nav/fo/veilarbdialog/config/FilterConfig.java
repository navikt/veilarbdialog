package no.nav.fo.veilarbdialog.config;

import no.nav.common.auth.oidc.filter.OidcAuthenticationFilter;
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.utils.ServiceUserTokenFinder;
import no.nav.common.auth.utils.UserTokenFinder;
import no.nav.common.log.LogFilter;
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static no.nav.common.auth.Constants.*;
import static no.nav.common.auth.oidc.filter.OidcAuthenticator.fromConfigs;

@Configuration
public class FilterConfig {

    private static final List<String> ALLOWED_SERVICE_USERS = List.of(
            "srvveilarbportefolje", "srvveilarbdirigent", "srvveilarboppfolging"
    );

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.cluster}")
    private String applicationCluster;

    @Value("${application.openam.clientId}")
    private String openAmClientId;

    @Value("${application.openam.discoveryUrl}")
    private String openAmDiscoveryUrl;

    @Value("${application.openam.refreshUrl}")
    private String openAmRefreshUrl;

    @Value("${application.sts.discovery.url}")
    private String naisStsDiscoveryUrl;

    @Value("${application.azure.ad.clientId}")
    private String azureAdClientId;

    @Value("${application.azure.ad.discoveryUrl}")
    private String azureAdDiscoveryUrl;

    @Value("${application.azure.b2c.clientId}")
    private String azureAdB2cClientId;

    @Value("${application.azure.b2c.discoveryUrl}")
    private String azureAdB2cDiscoveryUrl;

    private OidcAuthenticatorConfig openAmStsAuthConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(openAmDiscoveryUrl)
                .withClientId(openAmClientId)
                .withIdTokenFinder(new ServiceUserTokenFinder())
                .withIdentType(IdentType.Systemressurs);
    }

    private OidcAuthenticatorConfig naisStsAuthConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(naisStsDiscoveryUrl)
                .withClientIds(ALLOWED_SERVICE_USERS)
                .withIdentType(IdentType.Systemressurs);
    }

    private OidcAuthenticatorConfig openAmAuthConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(openAmDiscoveryUrl)
                .withClientId(openAmClientId)
                .withIdTokenCookieName(OPEN_AM_ID_TOKEN_COOKIE_NAME)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME)
                .withIdTokenFinder(new UserTokenFinder())
                .withRefreshUrl(openAmRefreshUrl)
                .withIdentType(IdentType.InternBruker);
    }

    private OidcAuthenticatorConfig azureAdAuthConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(azureAdDiscoveryUrl)
                .withClientId(azureAdClientId)
                .withIdTokenCookieName(AZURE_AD_ID_TOKEN_COOKIE_NAME)
                .withIdentType(IdentType.InternBruker);
    }

    private OidcAuthenticatorConfig azureAdB2CAuthConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(azureAdB2cDiscoveryUrl)
                .withClientId(azureAdB2cClientId)
                .withIdTokenCookieName(AZURE_AD_B2C_ID_TOKEN_COOKIE_NAME)
                .withIdentType(IdentType.EksternBruker);
    }

    @Bean
    public FilterRegistrationBean<LogFilter> logFilterRegistrationBean() {
        boolean dev = applicationCluster.contains("dev");
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LogFilter(applicationName, dev));
        //registration.setOrder(2);
        registration.setOrder(Integer.MIN_VALUE);
        registration.addUrlPatterns("/*");
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
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                fromConfigs(
                        openAmAuthConfig(),
                        azureAdAuthConfig(),
                        azureAdB2CAuthConfig(),
                        openAmStsAuthConfig(),
                        naisStsAuthConfig()
                )
        );
        registration.setFilter(authenticationFilter);
        registration.setOrder(3);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SetStandardHttpHeadersFilter> setStandardHeadersFilterRegistrationBean() {
        FilterRegistrationBean<SetStandardHttpHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetStandardHttpHeadersFilter());
        registration.setOrder(4);
        registration.addUrlPatterns("/*");
        return registration;
    }

}
