package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.db.jdbc.SisteOppfolgingsperiodeEntity;
import no.nav.fo.veilarbdialog.db.jdbc.SisteOppfolgingsperiodeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toLocalDateTime;
import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toZonedDateTime;

@Repository
@Slf4j
@RequiredArgsConstructor
class SistePeriodeDAO {

    private final SisteOppfolgingsperiodeRepository repository;

    Optional<Oppfolgingsperiode> hentSisteOppfolgingsPeriode(String aktorId) {
        return repository.findByAktorid(aktorId).map(this::mapRow);
    }

    void upsertOppfolgingsperiode(Oppfolgingsperiode oppfolgingsperiode) {
        Optional<Oppfolgingsperiode> gammelOppfolgingsperiode =
                hentSisteOppfolgingsPeriode(oppfolgingsperiode.aktorid());

        if (gammelOppfolgingsperiode.isEmpty()) {
            repository.insert(
                    oppfolgingsperiode.oppfolgingsperiode().toString(),
                    oppfolgingsperiode.aktorid(),
                    toLocalDateTime(oppfolgingsperiode.startTid()),
                    toLocalDateTime(oppfolgingsperiode.sluttTid()));
            log.info("opprettet oppfolgingsperiodeId {}", oppfolgingsperiode);

        } else if (!gammelOppfolgingsperiode.get().startTid().isAfter(oppfolgingsperiode.startTid())) {
            int antallOppdatert = repository.update(
                    oppfolgingsperiode.oppfolgingsperiode().toString(),
                    oppfolgingsperiode.aktorid(),
                    toLocalDateTime(oppfolgingsperiode.startTid()),
                    toLocalDateTime(oppfolgingsperiode.sluttTid()));
            if (antallOppdatert == 1) {
                log.info("oppdatert oppfolgingsperiode {}", oppfolgingsperiode);
            }
        } else {
            log.info("ignorerer oppfolgingsperiode {} fordi den er eldre enn eksisterende {}", oppfolgingsperiode, gammelOppfolgingsperiode.get());
        }
    }

    private Oppfolgingsperiode mapRow(SisteOppfolgingsperiodeEntity row) {
        return new Oppfolgingsperiode(
                row.getAktorid(),
                UUID.fromString(row.getPeriodeUuid()),
                toZonedDateTime(row.getStartdato()),
                toZonedDateTime(row.getSluttdato()));
    }
}
