package no.nav.fo.veilarbdialog.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.common.utils.UrlUtils.createServiceUrl;

@Configuration
public class AktorClientConfig {

    @Value("${application.pdl.api.url}")
    String pdlUrl;

    @Bean
    public AktorOppslagClient aktorClient(MachineToMachineTokenClient tokenClient) {
        String tokenScop = String.format("api://%s-fss.pdl.pdl-api/.default",
                isProduction().orElse(false) ? "prod" : "dev"
        );
        return new CachedAktorOppslagClient(new PdlAktorOppslagClient(
                pdlUrl,
                () -> tokenClient.createMachineToMachineToken(tokenScop))
        );
    }
}