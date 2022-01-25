package no.nav.fo.veilarbdialog.kvp;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.util.Optional;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@Service
public class KvpService {

    private final String baseUrl;
    private final OkHttpClient client;

    public KvpService(@Value("${application.veilarboppfolging.api.url}") String baseUrl, OkHttpClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    private KvpDTO get(String aktorId) throws IOException {
        var uri = String.format("%s/v2/kvp?aktorId=%s", baseUrl, aktorId);
        var request = new Request.Builder()
                .url(uri)
                .build();
        try (var response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);

            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return null;
            }

            return RestUtils.parseJsonResponse(response, KvpDTO.class).orElse(null);
        } catch (IOException e) {
            log.error("Unable to process request {}", uri, e);
            throw e;
        }

    }

    public String kontorsperreEnhetId(String aktorId) {
        try {
            return Optional.ofNullable(this.get(aktorId))
                    .map(KvpDTO::getEnhet)
                    .orElse(null);
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "veilarbdialog har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "veilarboppfolging kunne ikke prosessere en request.");
        }
    }

}
