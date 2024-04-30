package no.nav.fo.veilarbdialog.util;

import lombok.AllArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.db.dao.OwnerProviderDAO;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AllArgsConstructor
@Component
public class DialogOwnerProvider {
    private final OwnerProviderDAO ownerProviderDAO;
    private final AktorOppslagClient aktorOppslagClient;

    Optional<Fnr> getOwner(String dialogId) {
        try {
            return ownerProviderDAO.getDialogOwner(Long.parseLong(dialogId))
                    .map(aktorId -> aktorOppslagClient.hentFnr(aktorId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
