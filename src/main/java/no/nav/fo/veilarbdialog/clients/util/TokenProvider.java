package no.nav.fo.veilarbdialog.clients.util;

import lombok.RequiredArgsConstructor;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenProvider {


    final IAuthService auth;
    final AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient;
    final AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient;
    final TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient;

    public String get(String azureScope, String tokenXScope) {
        if (auth.erInternBruker()) {
            return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(azureScope, auth.getInnloggetBrukerToken());
        } else if (auth.erEksternBruker()) {
            return tokenXOnBehalfOfTokenClient.exchangeOnBehalfOfToken(tokenXScope, auth.getInnloggetBrukerToken());
        } else {
            return azureAdMachineToMachineTokenClient.createMachineToMachineToken(azureScope);
        }
    }
}
