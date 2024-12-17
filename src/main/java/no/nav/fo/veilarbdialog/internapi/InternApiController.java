package no.nav.fo.veilarbdialog.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.TilgangsType;
import no.nav.veilarbdialog.internapi.api.InternalApi;
import no.nav.veilarbdialog.internapi.model.Dialog;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class InternApiController implements InternalApi {

    private final InternApiService internApiService;
    private final IAuthService auth;

    @Override
    public ResponseEntity<Dialog> hentDialog(Integer dialogId) {
        DialogData dialogData = internApiService.hentDialog(dialogId);
        auth.sjekkTilgangTilPerson(AktorId.of(dialogData.getAktorId()), TilgangsType.LESE);
        Dialog dialog = InternDialogMapper.mapTilDialog(dialogData);

        return ResponseEntity.of(Optional.ofNullable(dialog));
    }

    @Override
    public ResponseEntity<List<Dialog>> hentDialoger(String aktorId, UUID oppfolgingsperiodeId) {
        List<DialogData> dialogData = internApiService.hentDialoger(aktorId, oppfolgingsperiodeId);
        dialogData
                .stream()
                .findAny()
                .map(DialogData::getAktorId)
                .map(AktorId::of)
                .ifPresent((dialogOwner) -> auth.sjekkTilgangTilPerson(dialogOwner, TilgangsType.LESE));

        List<Dialog> dialoger = dialogData
                .stream()
                .map(InternDialogMapper::mapTilDialog)
                .toList();

        return ResponseEntity.of(Optional.of(dialoger));
    }
}
