package no.nav.sbl.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import lombok.SneakyThrows;
import no.nav.common.json.DateModule;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;

import static org.glassfish.jersey.client.ClientProperties.*;

public class RestUtils {


    @SneakyThrows
    public static Client createClient() {
        return new JerseyClientBuilder()
                .sslContext(SSLContext.getDefault())
                .withConfig(createClientConfig())
                .build();
    }

    private static ClientConfig createClientConfig() {
        return new ClientConfig()
                .register(new JsonProvider())
                .register(DateModule.module())
                .property(FOLLOW_REDIRECTS, false)
                .property(CONNECT_TIMEOUT, 5000)
                .property(READ_TIMEOUT, 15000);
    }

    private static class JsonProvider extends JacksonJaxbJsonProvider {

        private JsonProvider() {
            ObjectMapper objectMapper = new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(DateModule.module())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                    .configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.setVisibility(objectMapper.getVisibilityChecker()
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
            );
        }
    }

}
