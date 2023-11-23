package no.nav.fo.veilarbdialog.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.fo.veilarbdialog.service.PersonService;
import no.nav.poao.dab.spring_auth.AuthService;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AuthConfig {

    @Bean
    PoaoTilgangClient poaoTilgangClient(@Value("${application.poao_tilgang.url}") String poaoTilgangApiUrl,
                                        @Value("${application.poao_tilgang.scope}") String scope,
                                        AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient) {
        PoaoTilgangHttpClient poaoTilgangHttpClient = new PoaoTilgangHttpClient(poaoTilgangApiUrl, () -> azureAdMachineToMachineTokenClient.createMachineToMachineToken(scope), RestClient.baseClient());
        return PoaoTilgangCachedClient.createDefaultCacheClient(poaoTilgangHttpClient);



    }
    @Bean
    IAuthService authService(AuthContextHolder authcontextHolder, PoaoTilgangClient poaoTilgangClient, PersonService personService) {
        return new AuthService(authcontextHolder, poaoTilgangClient, personService, "veilarbdialog");
    }
}
