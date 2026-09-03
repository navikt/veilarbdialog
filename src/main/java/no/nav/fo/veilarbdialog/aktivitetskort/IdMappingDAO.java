package no.nav.fo.veilarbdialog.aktivitetskort;


import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.db.jdbc.DialogRepository;
import no.nav.fo.veilarbdialog.domain.Arenaid;
import no.nav.fo.veilarbdialog.domain.TekniskId;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdMappingDAO {

    private final DialogRepository dialogRepository;

    public int migrerArenaDialogerTilTekniskId(Arenaid arenaId, TekniskId tekniskId) {
        return dialogRepository.migrerArenaTilTekniskId(tekniskId.getId(), arenaId.getId());
    }
}
