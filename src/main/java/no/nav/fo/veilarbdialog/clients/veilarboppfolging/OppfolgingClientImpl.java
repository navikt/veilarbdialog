package no.nav.fo.veilarbdialog.clients.veilarboppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.config.VeilarboppfolgingClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingClientImpl implements OppfolgingClient {

    private final VeilarboppfolgingClient veilarboppfolgingClient;

    @Override
    public Optional<ManuellStatusV2DTO> hentManuellStatus(Fnr fnr) {
        String uri = String.format("/v2/manuell/status?fnr=%s", fnr.get());
        return veilarboppfolgingClient.request(uri, ManuellStatusV2DTO.class);
    }

    @Override
    public boolean erUnderOppfolging(Fnr fnr) {
        String uri = String.format("/v2/oppfolging?fnr=%s", fnr.get());

        return veilarboppfolgingClient
                .request(uri, UnderOppfolgingDTO.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot oppfolging"))
                .isErUnderOppfolging();
    }

}
