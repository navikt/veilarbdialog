package no.nav.fo.veilarbdialog.rest;

import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AbacHelsesjekk implements Pingable {

    private Pep pep;

    @Inject
    public AbacHelsesjekk(Pep pep) {
        this.pep = pep;
    }

    @Override
    public Ping ping() {
        PingMetadata metadata = new PingMetadata(
                "ABAC via " + System.getProperty("abac.endpoint.url", "mock"),
                "Tilgangskontroll som sjekker om veileder har tilgang til bruker",
                true
        );

        try {
            pep.ping();
        } catch (PepException e) {
            return Ping.feilet(metadata, e);
        }
        return Ping.lyktes(metadata);
    }
}
