package no.nav.fo.veilarbdialog.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.feil.Feil;
import no.nav.common.types.feil.FeilType;
import no.nav.fo.veilarbdialog.domain.KvpDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class KvpService {

    private final String baseUrl;
    private final OkHttpClient client;
    private final SystemUserTokenProvider systemUserTokenProvider;

    private KvpDTO get(String aktorId) {

        String uri = String.format("%s/kvp/%s/currentStatus", baseUrl, aktorId);
        Request request = new Request.Builder()
                .url(uri)
                .header("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken())
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, KvpDTO.class).orElse(null);
        } catch (IOException e) {
            log.error("Unable to process request {}", uri, e);
            throw new RuntimeException(e);
        }

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
