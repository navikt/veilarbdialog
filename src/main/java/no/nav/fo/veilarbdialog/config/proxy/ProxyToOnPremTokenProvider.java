package no.nav.fo.veilarbdialog.config.proxy;

import lombok.RequiredArgsConstructor;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProxyToOnPremTokenProvider {
    private final IAuthService authService;
    private final AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient;
    private final MachineToMachineTokenClient machineToMachineTokenClient;
    private final TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient;

    private final boolean isProd = EnvironmentUtils.isProduction().orElse(false);
    private final String scope = String.format("api://%s-fss.pto.veilarbdialog/.default", isProd ? "prod" : "dev");
    private final String audience = String.format("%s-fss:pto:veilarbdialog", isProd ? "prod" : "dev");

    public String getProxyToken() {
        if (authService.erInternBruker()) return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, authService.getInnloggetBrukerToken());
        else if (authService.erEksternBruker()) return tokenXOnBehalfOfTokenClient.exchangeOnBehalfOfToken(audience, authService.getInnloggetBrukerToken());
        else if (authService.erSystemBruker()) return machineToMachineTokenClient.createMachineToMachineToken(scope);
        else throw new RuntimeException("Klarte ikke å identifisere brukertype");
    }
}
