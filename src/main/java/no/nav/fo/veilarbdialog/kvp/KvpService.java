package no.nav.fo.veilarbdialog.kvp;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class KvpService {
    private final HttpClientWrapper veilarboppfolgingClientWrapper;

    public String kontorsperreEnhetId(String aktorId) {
        try {
            var path = String.format("/v2/kvp?aktorId=%s", aktorId);
            return veilarboppfolgingClientWrapper
                    .get(path, KvpDTO.class)
                    .map(KvpDTO::getEnhet)
                    .orElse(null);
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "veilarbdialog har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }
    }
}
