package no.nav.fo.veilarbdialog.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.utils.UrlUtils;
import no.nav.fo.veilarbdialog.auth.AuthService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
public class VeilarboppfolgingClient {
    private final Function<Boolean, String> tokenProvider;
    private final OkHttpClient client;

    @Value("${application.veilarboppfolging.api.url}")
    private String baseUrl;

    public VeilarboppfolgingClient(
            @Value("${application.veilarboppfolging.api.scope}") String veilarboppfolgingapiScope,
            AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient,
            SystemUserTokenProvider systemUserTokenProvider,
            OkHttpClient client,
            AuthService auth) {
        this.tokenProvider = (Boolean useSystemTokenOverride) -> {
            if (!useSystemTokenOverride && auth.erInternBruker()) {
                return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(veilarboppfolgingapiScope, auth.getInnloggetBrukerToken());
            } else {
                return systemUserTokenProvider.getSystemUserToken();
            }
        };
        this.client = client;
    }

    private Request buildRequest(String path) {
        return buildRequest(path, false);
    }

        @NotNull
    private Request buildRequest(String path, Boolean useSystemTokenOverride) {
        String uri = UrlUtils.joinPaths(baseUrl, path);
        var token = tokenProvider.apply(useSystemTokenOverride);
        if (token == null) throw new IllegalStateException("Token can not be null");
        return new Request.Builder()
                .url(uri)
                .header(AUTHORIZATION, "Bearer " + token)
                .build();
    }

    public <T> Optional<T> request(String path, Class<T> classOfT) {
            return request(path, classOfT, false);
    }

    public <T> Optional<T> request(String path, Class<T> classOfT, boolean useSystemTokenOverride) {
        Request request = buildRequest(path);
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, classOfT);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }


    public <T> Optional<List<T>> requestArrayData(String path, Class<T> classOfT) {
        Request request = buildRequest(path);
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonArrayResponse(response, classOfT);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    private ResponseStatusException internalServerError(Exception cause, String url) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Feil ved kall mot %s", url), cause);
    }

}
