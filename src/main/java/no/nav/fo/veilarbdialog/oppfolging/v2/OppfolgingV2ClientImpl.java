package no.nav.fo.veilarbdialog.oppfolging.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingV2ClientImpl implements OppfolgingV2Client {
    private final OkHttpClient client;
    private final AktorOppslagClient aktorOppslagClient;


    @Value("${application.kvp.url}")
    private String baseUrl;

    public Optional<OppfolgingV2UnderOppfolgingDTO> fetchUnderoppfolging(AktorId aktorId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktorId);

        String uri = String.format("%s/v2/oppfolging?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, OppfolgingV2UnderOppfolgingDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    @Override
    public Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(AktorId aktorId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktorId);

        String uri = String.format("%s/v2/oppfolging/periode/gjeldende?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }
            return RestUtils.parseJsonResponse(response, OppfolgingPeriodeMinimalDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    @Override
    public Optional<List<OppfolgingPeriodeMinimalDTO>> hentOppfolgingsperioder(AktorId aktorId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktorId);

        String uri = String.format("%s/v2/oppfolging/perioder?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }
            return RestUtils.parseJsonArrayResponse(response, OppfolgingPeriodeMinimalDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    private ResponseStatusException internalServerError(Exception cause, String url) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Feil ved kall mot %s", url), cause);
    }
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
