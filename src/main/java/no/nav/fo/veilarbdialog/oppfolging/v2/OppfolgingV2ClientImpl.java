package no.nav.fo.veilarbdialog.oppfolging.v2;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.kvp.KvpDTO;
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper;
import no.nav.fo.veilarbdialog.oppfolging.siste_periode.GjeldendePeriodeMetrikk;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingV2ClientImpl implements OppfolgingV2Client {
    private final AktorOppslagClient aktorOppslagClient;
    private final GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk;
    private final HttpClientWrapper veilarboppfolgingClientWrapper;

    public Optional<OppfolgingV2UnderOppfolgingDTO> fetchUnderoppfolging(AktorId aktorId) {
        var fnr = aktorOppslagClient.hentFnr(aktorId);
        var uri = String.format("/v2/oppfolging?fnr=%s", fnr.get());
        return veilarboppfolgingClientWrapper.get(uri, OppfolgingV2UnderOppfolgingDTO.class);
    }

    @Override
    @Timed
    public Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(AktorId aktorId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktorId);
        String uri = String.format("/v2/oppfolging/periode/gjeldende?fnr=%s", fnr.get());
        var response = veilarboppfolgingClientWrapper.get(uri, OppfolgingPeriodeMinimalDTO.class);
        gjeldendePeriodeMetrikk.tellKallTilEksternOppfolgingsperiode(response.isPresent());
        return response;
    }

    @Timed
    @Override
    public Optional<List<OppfolgingPeriodeMinimalDTO>> hentOppfolgingsperioder(AktorId aktorId) {
        Fnr fnr = aktorOppslagClient.hentFnr(aktorId);
        String uri = String.format("/v2/oppfolging/perioder?fnr=%s", fnr.get());
        return veilarboppfolgingClientWrapper.getList(uri, OppfolgingPeriodeMinimalDTO.class);
    }

    @Timed
    @Override
    public String hentKVPKontorEnhet(String aktorId) {
        var path = String.format("/v2/kvp?aktorId=%s", aktorId);
        return veilarboppfolgingClientWrapper
                .get(path, KvpDTO.class)
                .map(KvpDTO::getEnhet)
                .orElse(null);
    }
}
