package no.nav.fo.veilarbdialog.clients.veilarbperson;

import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VeilarbpersonClientImpl implements VeilarbpersonClient {
    private final HttpClientWrapper veilarbpersonClientWrapper;

    public VeilarbpersonClientImpl(HttpClientWrapper veilarbpersonClientWrapper) {
        this.veilarbpersonClientWrapper = veilarbpersonClientWrapper;
    }

    @Override
    public Optional<Nivaa4DTO> hentNiva4(Fnr fnr) {
        String uri = String.format("/person/%s/harNivaa4", fnr.get());
        return veilarbpersonClientWrapper.get(uri, Nivaa4DTO.class);
    }

}
