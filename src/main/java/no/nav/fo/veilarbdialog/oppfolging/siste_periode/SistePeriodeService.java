package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class SistePeriodeService {
    private final OppfolgingV2Client oppfolgingV2Client;
    private final SistePeriodeDAO sistePeriodeDAO;

    @Timed
    public UUID hentGjeldendeOppfolgingsperiodeMedFallback(AktorId aktorId) {

        Supplier<IngenGjeldendePeriodeException> exceptionSupplier = () -> new IngenGjeldendePeriodeException(String.format("AktorId: %s har ingen gjeldende oppfølgingsperiode", aktorId.get()));

        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId.get())
                // Mangler aktiv oppfølgingsperiode
                .filter(periode -> periode.sluttTid()  == null)
                .or(() -> oppfolgingV2Client.fetchGjeldendePeriode(aktorId)
                        .map(
                                dto -> new Oppfolgingsperiode(aktorId.get(), dto.getUuid(), dto.getStartDato(), dto.getSluttDato())
                        )
                ).orElseThrow(exceptionSupplier);

        return oppfolgingsperiode.oppfolgingsperiode();
    }

}
