package no.nav.fo.veilarbdialog.internal;

import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Configuration
public class IssoHelsesjekk implements Pingable {
    @Override
    public Ping ping() {
        PingMetadata metadata = new PingMetadata(
                "ISSO via " + System.getProperty("isso.isalive.url"),
                "Sjekker om is-alive til ISSO svarer. Single-signon pålogging.",
                true
        );

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("isso.isalive.url")).openConnection();
            connection.connect();
            if (connection.getResponseCode() == 200) {
                return Ping.lyktes(metadata);
            }
            return Ping.feilet(metadata, "Isalive returnerte statuskode: " + connection.getResponseCode());
        } catch (IOException e) {
            return Ping.feilet(metadata, e);
        }
    }
}
