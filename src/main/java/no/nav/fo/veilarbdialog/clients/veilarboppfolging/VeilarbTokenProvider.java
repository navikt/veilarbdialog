package no.nav.fo.veilarbdialog.clients.veilarboppfolging;

import lombok.RequiredArgsConstructor;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.fo.veilarbdialog.auth.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class VeilarbTokenProvider implements Supplier<String> {


    final AuthService auth;
    final AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient;
    final AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient;
    final TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient;
    @Value("${application.veilarboppfolging.api.azureScope}")
    String azureScope;
    @Value("${application.veilarboppfolging.api.tokenXScope}")
    String tokenXScope;

    @Override
    public String get() {
        if (auth.erInternBruker()) {
            return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(azureScope, auth.getInnloggetBrukerToken());
        } else if (auth.erEksternBruker()) {
            return tokenXOnBehalfOfTokenClient.exchangeOnBehalfOfToken(tokenXScope, auth.getInnloggetBrukerToken());
        } else {
            return azureAdMachineToMachineTokenClient.createMachineToMachineToken(azureScope);
        }
    }
}
