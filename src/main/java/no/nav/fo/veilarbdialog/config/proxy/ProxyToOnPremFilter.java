package no.nav.fo.veilarbdialog.config.proxy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.https;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyToOnPremFilter {
    private final ProxyToOnPremTokenProvider proxyToOnPremTokenProvider;

    @Value("${veilarbdialog-fss.url}")
    private String veilarbdialogFssUrl;

    private Function<ServerRequest, ServerRequest> oboExchange(Supplier<String> getToken) {
        return request -> {
            log.info("Gateway obo " + request);
            ServerRequest.Builder requestBuilder = ServerRequest.from(request);
            List<String> oldAuthHeaderValues = request.headers().header(HttpHeaders.AUTHORIZATION);
            requestBuilder.headers(headers -> {
                headers.replace(HttpHeaders.AUTHORIZATION, oldAuthHeaderValues, List.of("Bearer " + getToken.get()));
            });
            return requestBuilder.build();
        };
    }

    @ConditionalOnProperty(name = "spring.cloud.gateway.mvc.enabled", havingValue = "true")
    @Order(-1)
    @Bean
    public RouterFunction<ServerResponse> getRoute() {
        var sendToOnPrem = https(URI.create(veilarbdialogFssUrl));
        return route()
                .route(path("/internal/isAlive")
                        .or(path("/internal/isReady"))
                        .or(path("/internal/selftest"))
                        .negate(), sendToOnPrem)
                .before(oboExchange(() -> proxyToOnPremTokenProvider.getProxyToken()))
                .onError((Throwable error) -> {
                            log.error("Proxy error", error);
                            return true;
                        },
                        ((Throwable error, ServerRequest request) -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getStackTrace().toString()))
                )
                .build();
    }
}
