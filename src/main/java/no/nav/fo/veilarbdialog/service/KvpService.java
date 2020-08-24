package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.feil.Feil;
import no.nav.common.types.feil.FeilType;
import no.nav.fo.veilarbdialog.domain.KvpDTO;
import org.springframework.stereotype.Service;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KvpService {

    private final String baseUrl;
    private final Client client;
    private final SystemUserTokenProvider systemUserTokenProvider;

    private KvpDTO get(String aktorId) {
        String uri = String.format("%s/kvp/%s/currentStatus", baseUrl, aktorId);
        Invocation.Builder b = client.target(uri).request();
        b.header("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken());
        return b.get(KvpDTO.class);
    }

    public String kontorsperreEnhetId(String aktorId) {
        try {
            return Optional.ofNullable(this.get(aktorId))
                    .map(KvpDTO::getEnhet)
                    .orElse(null);
        } catch (ForbiddenException e) {
            throw new Feil(FeilType.UKJENT, "veilarbdialog har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new Feil(FeilType.UKJENT, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }
    }

}
