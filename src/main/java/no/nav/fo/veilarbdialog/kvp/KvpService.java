package no.nav.fo.veilarbdialog.kvp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.config.VeilarboppfolgingClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class KvpService {

    private final VeilarboppfolgingClient veilarboppfolgingClient;

    public String kontorsperreEnhetId(String aktorId) {
        try {
            var path = String.format("/v2/kvp?aktorId=%s", aktorId);
            return veilarboppfolgingClient
                    .request(path, KvpDTO.class)
                    .map(KvpDTO::getEnhet)
                    .orElse(null);
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "veilarbdialog har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }
    }

}
