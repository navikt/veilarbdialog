package no.nav.fo.veilarbdialog.oppfolging.v2;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.GjeldendePeriodeMetrikk;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OppfolgingV2ClientConfig {

    @Value("${application.veilarboppfolging.api.scope}")
    private String veilarboppfolgingapi_scope;

    @Bean
    public OppfolgingV2Client oppfolgingV2ClientImpl(AktorOppslagClient aktorOppslagClient, GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk, AzureAdMachineToMachineTokenClient tokenClient) {
        OkHttpClient okHttpClient = RestClient.baseClient();

        return new OppfolgingV2ClientImpl(okHttpClient, aktorOppslagClient, gjeldendePeriodeMetrikk, () -> tokenClient.createMachineToMachineToken(veilarboppfolgingapi_scope));
    }
}
