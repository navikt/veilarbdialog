package no.nav.fo.veilarbdialog.oppfolging.v2;


import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.clients.veilarboppfolging.ManuellStatusV2DTO;

import java.util.List;
import java.util.Optional;



public interface OppfolgingV2Client {

    Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(AktorId aktorId);

    Optional<List<OppfolgingPeriodeMinimalDTO>> hentOppfolgingsperioder(AktorId aktorId);

    Optional<ManuellStatusV2DTO> hentManuellStatus(Fnr fnr);

    boolean erUnderOppfolging(Fnr fnr);
}
