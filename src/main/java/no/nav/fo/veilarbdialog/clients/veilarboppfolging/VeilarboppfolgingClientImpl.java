package no.nav.fo.veilarbdialog.clients.veilarboppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RequiredArgsConstructor
public class VeilarboppfolgingClientImpl implements VeilarboppfolgingClient {

    private final String baseUrl;

    private final OkHttpClient client;

    private final Supplier<String> machineToMachineTokenProvider;

    public Optional<ManuellStatusV2DTO> hentManuellStatus(Fnr fnr) {
        String uri = String.format("%s/v2/manuell/status?fnr=%s", baseUrl, fnr.get());

        Request request = new Request.Builder()
                .url(uri)
                .header(AUTHORIZATION, "Bearer " + machineToMachineTokenProvider.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, ManuellStatusV2DTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }

    @Override
    public boolean erUnderOppfolging(Fnr fnr) {
        String uri = String.format("%s/v2/oppfolging?fnr=%s", baseUrl, fnr.get());

        Request request = new Request.Builder()
                .url(uri)
                .header(AUTHORIZATION, "Bearer " + machineToMachineTokenProvider.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);

            return RestUtils.parseJsonResponse(response, UnderOppfolgingDTO.class)
                    .orElseThrow().isErUnderOppfolging();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }

}
