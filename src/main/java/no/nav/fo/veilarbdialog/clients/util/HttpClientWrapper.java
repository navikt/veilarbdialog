package no.nav.fo.veilarbdialog.clients.util;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.utils.UrlUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
public class HttpClientWrapper {
    private final OkHttpClient client;

    private final String baseUrl;

    public HttpClientWrapper(OkHttpClient client, String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    @NotNull
    private Request buildRequest(String path) {
        String uri = UrlUtils.joinPaths(baseUrl, path);
        return new Request.Builder()
                .url(uri)
                .build();
    }

    public <T> Optional<T> get(String path, Class<T> classOfT) {
        Request request = buildRequest(path);
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, classOfT);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    public void post(String path, RequestBody body) {
        Request request = buildRequest(path).newBuilder()
            .post(body).build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    public <T> Optional<T> postAndReceive(String path, RequestBody body, Class<T> classOfT) {
        Request request = buildRequest(path).newBuilder()
                .post(body).build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, classOfT);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    public <T> Optional<List<T>> postAndReceiveList(String path, RequestBody body, Class<T> classOfT) {
        Request request = buildRequest(path).newBuilder()
                .post(body).build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonArrayResponse(response, classOfT);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }


    public <T> Optional<List<T>> getList(String path, Class<T> classOfT) {
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
