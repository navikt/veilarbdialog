package no.nav.fo.veilarbdialog.client;

import no.nav.apiapp.feil.Feil;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import java.util.Optional;

public class KvpClient {

    private final String baseUrl;
    private final Client client;

    public KvpClient(String baseUrl, Client client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

    private KvpDTO get(String aktorId) {
        String uri = String.format("%s/kvp/%s/currentStatus", baseUrl, aktorId);
        Invocation.Builder b = client.target(uri).request();
        b.header("Authorization", "Bearer " + this.systemUserTokenProvider.getToken());
        return b.get(KvpDTO.class);
    }

    public String kontorsperreEnhetId(String aktorId) {
        try {
            return Optional.ofNullable(this.get(aktorId))
                    .map(KvpDTO::getEnhet)
                    .orElse(null);
        } catch (ForbiddenException e) {
            throw new Feil(Feil.Type.UKJENT, "veilarbdialog har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new Feil(Feil.Type.UKJENT, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }
    }
}
