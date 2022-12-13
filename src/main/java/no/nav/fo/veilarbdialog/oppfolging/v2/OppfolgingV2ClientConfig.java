package no.nav.fo.veilarbdialog.oppfolging.v2;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.GjeldendePeriodeMetrikk;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

public class OppfolgingV2ClientConfig {

    @Bean
    public OppfolgingV2Client oppfolgingV2ClientImpl(AktorOppslagClient aktorOppslagClient, GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk, AzureAdMachineToMachineTokenClient tokenClient) {
        String tokenScope = String.format(
                "api://%s-fss.pto.veilarboppfolging/.default",
                isProduction().orElse(false) ? "prod" : "dev"
        );

        OkHttpClient okHttpClient = RestClient.baseClient();

        return new OppfolgingV2ClientImpl(okHttpClient, aktorOppslagClient, gjeldendePeriodeMetrikk, () -> tokenClient.createMachineToMachineToken(tokenScope));
    }
}
