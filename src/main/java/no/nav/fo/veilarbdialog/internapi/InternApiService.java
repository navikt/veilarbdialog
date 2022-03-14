package no.nav.fo.veilarbdialog.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternApiService {

    private final AuthService authService;
    private final DialogDAO dialogDAO;
    private final KontorsperreFilter kontorsperreFilter;

    public DialogData hentDialog(Integer dialogId) {
        if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        DialogData dialogData = dialogDAO.hentDialog(dialogId);
        authService.harTilgangTilPersonEllerKastIngenTilgang(dialogData.getAktorId());

        return filtrerKontorsperret(dialogData);
    }

   public List<DialogData> hentDialoger(String aktorId, UUID oppfolgingsperiodeId) {
       if (aktorId == null && oppfolgingsperiodeId == null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
       }

       if (authService.erEksternBruker()) {
           throw new ResponseStatusException(HttpStatus.FORBIDDEN);
       }

       if (oppfolgingsperiodeId == null) {
           return hentDialogerForAktorId(aktorId);
       }

       if (aktorId == null) {
           return hentDialogerForOppfolgingsperiodeId(oppfolgingsperiodeId);
       }

       return hentDialogerForAktorId(aktorId)
               .stream()
               .filter(d -> d.getOppfolgingsperiode().toString().equals(oppfolgingsperiodeId.toString()))
               .toList();
   }

   private List<DialogData> hentDialogerForAktorId(String aktorId) {
        authService.harTilgangTilPersonEllerKastIngenTilgang(aktorId);
        return filtrerKontorsperret(dialogDAO.hentDialogerForAktorId(aktorId));
   }

   private List<DialogData> hentDialogerForOppfolgingsperiodeId(UUID oppfolgingsperiodeId) {
        List<DialogData> dialoger = dialogDAO.hentDialogerForOppfolgingsperiodeId(oppfolgingsperiodeId);
        if (dialoger.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        authService.harTilgangTilPersonEllerKastIngenTilgang(dialoger.get(0).getAktorId());
        return filtrerKontorsperret(dialoger);
   }

    private List<DialogData> filtrerKontorsperret(List<DialogData> dialoger) {
        return dialoger
                .stream()
                .filter(kontorsperreFilter::tilgangTilEnhet)
                .map(dialog ->
                        dialog.withHenvendelser(
                                dialog.getHenvendelser().stream().filter(
                                        kontorsperreFilter::tilgangTilEnhet
                                ).toList()
                        )
                ).toList();
    }

    private DialogData filtrerKontorsperret(DialogData dialog) {
        return filtrerKontorsperret(List.of(dialog)).stream().findFirst().orElse(null);
    }
}
