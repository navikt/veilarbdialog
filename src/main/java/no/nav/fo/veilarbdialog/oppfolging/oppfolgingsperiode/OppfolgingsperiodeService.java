package no.nav.fo.veilarbdialog.oppfolging.oppfolgingsperiode;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingPeriodeMinimalDTO;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingV2Client;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OppfolgingsperiodeService {
    private final OppfolgingsperiodeDao dao;
    private final OppfolgingV2Client client;

    @Transactional
    @Timed(value = "oppfolgingsperiodeAdder", histogram = true)
    public boolean addOppfolgingsperioderForEnBruker() {
        AktorId aktorId = dao.hentEnBrukerUtenOppfolgingsperiode();

        if (aktorId == null) {
            return false;
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingperioder;
        try {
            oppfolgingperioder = client
                    .hentOppfolgingsperioder(aktorId)
                    .orElse(List.of()); //Finnes bruker uten oppfolginsperioder

        } catch (IngenGjeldendeIdentException e) {
            dao.setUkjentAktorId(aktorId);
            log.warn("ukjent aktorId {}", aktorId);
            return true;
        }
        if(oppfolgingperioder.isEmpty()) {
            dao.setIngenPerioder(aktorId);
            return true;
        }
        if(oppfolgingperioder.size() == 1 ) {
            dao.setAlleTilPeriode(aktorId, oppfolgingperioder.get(0).getUuid());
            return  true;
        }


        for (OppfolgingPeriodeMinimalDTO oppfolgingsperiode : oppfolgingperioder) {
            long raderOppdatert = dao.oppdaterAktiviteterForPeriode(aktorId, oppfolgingsperiode.getStartDato(), oppfolgingsperiode.getSluttDato(), oppfolgingsperiode.getUuid());
            log.info("lagt til oppfolgingsperiodeId={} i {} antall aktivitetsversjoner for aktorid={}", oppfolgingsperiode.getUuid(), raderOppdatert, aktorId.get());
        }

        dao.setOppfolgingsperiodeTilUkjentForGamleAktiviteterUtenOppfolgingsperiode(aktorId);

        return true;
    }
}
