package no.nav.fo.veilarbdialog.auth;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPepFactory;
import no.nav.common.abac.audit.AuditLogFilterUtils;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.Credentials;
import no.nav.fo.veilarbdialog.service.PersonService;
import no.nav.poao.dab.spring_auth.AuthService;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.IPersonService;
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.function.Supplier;

import static no.nav.common.abac.audit.AuditLogFilterUtils.not;

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
        return new AuthService(authcontextHolder, poaoTilgangClient, personService);
    }
}
