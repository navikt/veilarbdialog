package no.nav.fo.veilarbdialog.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.domain.Arenaid;
import no.nav.fo.veilarbdialog.domain.TekniskId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdMappingService {
    private final IdMappingDAO idMappingDAO;

    public void migrerArenaDialogerTilTekniskId(Arenaid arenaId, TekniskId tekniskId) {
        int updated = idMappingDAO.migrerArenaDialogerTilTekniskId(arenaId, tekniskId);

        if (updated > 1) {
            log.error("Fant flere dialoger på samme arenaId, antall={}, arenaId={} og tekniskId={}", updated, arenaId.getId(), tekniskId.getId());
        } else {
            log.info("Migrerte {} dialog med arenaId={} og tekniskId={}", updated, arenaId.getId(), tekniskId.getId());
        }
    }

}
