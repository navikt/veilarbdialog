package no.nav.fo.veilarbdialog.ws.consumer;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

@Component
public class AktoerHelsesjekk implements Helsesjekk {

//    @Inject
//    private Aktoer_v2PortType aktoer_v2PortType;

    @Override
    public void helsesjekk() {

//        aktoer_v2PortType.ping();
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        String aktoerUrl = System.getProperty("aktoer.endpoint.url");
        return new HelsesjekkMetadata(
                "virksomhet:Aktoer_v2 via " + aktoerUrl,
                "Ping av aktoer_v2 (hente aktørid).",
                true
        );
    }

}
