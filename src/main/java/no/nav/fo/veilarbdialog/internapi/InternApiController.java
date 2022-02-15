package no.nav.fo.veilarbdialog.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbdialog.internapi.api.InternalApi;
import no.nav.veilarbdialog.internapi.model.Dialog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class InternApiController implements InternalApi {

    private final InternApiService internApiService;

    @Override
    public ResponseEntity<Dialog> hentDialog(Integer dialogId) {
        Dialog dialog = Optional.of(internApiService.hentDialog(dialogId))
                .map(InternDialogMapper::mapTilDialog)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT));

        return ResponseEntity.of(Optional.of(dialog));
    }

    @Override
    public ResponseEntity<List<Dialog>> hentDialoger(String aktorId, UUID oppfolgingsperiodeId) {
        List<Dialog> dialoger = Optional.of(internApiService.hentDialoger(aktorId, oppfolgingsperiodeId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT))
                .stream()
                .map(InternDialogMapper::mapTilDialog)
                .toList();

        return ResponseEntity.of(Optional.of(dialoger));
    }
}
