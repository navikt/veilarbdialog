package no.nav.fo.veilarbdialog.config;

import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.poao.dab.spring_auth.AuthService;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.IPersonService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@Configuration
public class ApplicationTestConfig {
    @Bean
    public String pdlUrl(Environment environment) {
        return environment.getRequiredProperty("application.pdl.api.url");
    }

    @Bean
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        AzureAdMachineToMachineTokenClient tokenClient = mock(AzureAdMachineToMachineTokenClient.class);
        Mockito.when(tokenClient.createMachineToMachineToken(any())).thenReturn("mockMachineToMachineToken");
        return tokenClient;
    }

    @Bean
    public TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient() {
        TokenXOnBehalfOfTokenClient client = mock(TokenXOnBehalfOfTokenClient.class);
        Mockito.when(client.exchangeOnBehalfOfToken(any(), any())).thenReturn("mockMachineToMachineToken");
        return client;
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        AzureAdOnBehalfOfTokenClient tokenClient = mock(AzureAdOnBehalfOfTokenClient.class);
        Mockito.when(tokenClient.exchangeOnBehalfOfToken(any(), any())).thenReturn("mockMachineToMachineToken");
        return tokenClient;
    }

    @Bean
    UnleashClient unleashClient() {
        return mock(UnleashClient.class);
    }

    @Bean
    public IAuthService authService(AuthContextHolder authContextHolder, Pep pep, IPersonService personService) {
        return new AuthService(authContextHolder, pep, personService);
    }
}
