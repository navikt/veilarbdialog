package no.nav.fo.veilarbdialog.db.dao;

import lombok.AllArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.db.jdbc.DialogRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@AllArgsConstructor
@Repository
public class OwnerProviderDAO {

    DialogRepository dialogRepository;

    public Optional<AktorId> getDialogOwner(long dialogId) {
        return dialogRepository.findAktorIdByDialogId(dialogId)
            .stream().findFirst().map(AktorId::of);
    }
}
