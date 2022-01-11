package no.nav.fo.veilarbdialog.oppfolging.v2;


import no.nav.common.types.identer.AktorId;

import java.util.List;
import java.util.Optional;



public interface OppfolgingV2Client {
    Optional<OppfolgingV2UnderOppfolgingDTO> fetchUnderoppfolging(AktorId aktorId);

    Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(AktorId aktorId);

    Optional<List<OppfolgingPeriodeMinimalDTO>> hentOppfolgingsperioder(AktorId aktorId);
}
