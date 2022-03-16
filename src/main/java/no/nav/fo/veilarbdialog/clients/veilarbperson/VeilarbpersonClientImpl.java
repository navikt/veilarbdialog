package no.nav.fo.veilarbdialog.clients.veilarbperson;

import lombok.RequiredArgsConstructor;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RequiredArgsConstructor
public class VeilarbpersonClientImpl implements VeilarbpersonClient {

    private final String baseUrl;

    private final OkHttpClient client;

    @Override
    public Optional<Nivaa4DTO> hentNiva4(Fnr fnr) {

        String uri = String.format("%s/person/%s/harNivaa4", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);

            return RestUtils.parseJsonResponse(response, Nivaa4DTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }

}
