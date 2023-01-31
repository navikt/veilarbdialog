package no.nav.fo.veilarbdialog.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao.dab.spring_auth.IPersonService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService implements IPersonService {

    private final AktorOppslagClient aktorOppslagClient;

    @NotNull
    @Override
    public Fnr getFnrForAktorId(@NotNull EksternBrukerId eksternBrukerId) {
        if (eksternBrukerId instanceof Fnr fnr) return fnr;
        if (eksternBrukerId instanceof AktorId aktorId) return aktorOppslagClient.hentFnr(aktorId);
        throw new IllegalStateException("Kan bare hente fnr for AktorId eller Fnr");
    }

    @NotNull
    @Override
    public AktorId getAktorIdForPersonBruker(@NotNull EksternBrukerId eksternBrukerId) {
        if (eksternBrukerId instanceof AktorId aktorId) return aktorId;
        if (eksternBrukerId instanceof Fnr fnr) return aktorOppslagClient.hentAktorId(fnr);
        throw new IllegalStateException("Kan bare hente aktorId for AktorId eller Fnr");
    }

}
