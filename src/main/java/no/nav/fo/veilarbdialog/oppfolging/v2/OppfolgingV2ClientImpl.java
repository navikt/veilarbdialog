package no.nav.fo.veilarbdialog.oppfolging.v2;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.ManuellStatusV2DTO;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.UnderOppfolgingDTO;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.GjeldendePeriodeMetrikk;
import no.nav.fo.veilarbdialog.rest.FnrDto;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingV2ClientImpl implements OppfolgingV2Client {
    private final AktorOppslagClient aktorOppslagClient;
    private final GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk;
    private final HttpClientWrapper veilarboppfolgingClientWrapper;

    @Override
    public boolean erUnderOppfolging(Fnr fnr) {
        String uri = "/v3/hent-oppfolging";
        var fnrDto = new FnrDto(fnr.get());
        var requestBody = RequestBody.create(JsonUtils.toJson(fnrDto), MediaType.parse("application/json"));
        return veilarboppfolgingClientWrapper
                .postAndReceive(uri, requestBody, UnderOppfolgingDTO.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot oppfolging"))
                .isErUnderOppfolging();
    }

    @Override
    @Timed
    public Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(AktorId aktorId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktorId);
        String uri = "/v3/oppfolging/periode/hent-gjeldende-periode";
        var fnrDto = new FnrDto(fnr.get());
        var requestBody = RequestBody.create(JsonUtils.toJson(fnrDto), MediaType.parse("application/json"));
        var response = veilarboppfolgingClientWrapper.postAndReceive(uri, requestBody, OppfolgingPeriodeMinimalDTO.class);
        gjeldendePeriodeMetrikk.tellKallTilEksternOppfolgingsperiode(response.isPresent());
        return response;
    }

    @Timed
    @Override
    public Optional<List<OppfolgingPeriodeMinimalDTO>> hentOppfolgingsperioder(AktorId aktorId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktorId);
        String uri = "/v3/oppfolging/hent-perioder";
        var fnrDto = new FnrDto(fnr.get());
        var requestBody = RequestBody.create(JsonUtils.toJson(fnrDto), MediaType.parse("application/json"));
        return veilarboppfolgingClientWrapper.postAndReceiveList(uri, requestBody, OppfolgingPeriodeMinimalDTO.class);
    }

    @Override
    public Optional<ManuellStatusV2DTO> hentManuellStatus(Fnr fnr) {
        String uri = "/v3/manuell/hent-status";
        var fnrDto = new FnrDto(fnr.get());
        var requestBody = RequestBody.create(JsonUtils.toJson(fnrDto), MediaType.parse("application/json"));
        return veilarboppfolgingClientWrapper.postAndReceive(uri, requestBody, ManuellStatusV2DTO.class);
    }
}
