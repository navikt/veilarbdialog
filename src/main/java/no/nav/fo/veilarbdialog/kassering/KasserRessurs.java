package no.nav.fo.veilarbdialog.kassering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Id;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping(
        value = "/api/kassering",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Slf4j
public class KasserRessurs {

    private final DialogDAO dialogDAO;
    private final IAuthService auth;

    private final String godkjenteIdenter = EnvironmentUtils.getOptionalProperty("VEILARB_KASSERING_IDENTER").orElse("");

    private void sjekkErInternbruker() {
        if (!auth.erInternBruker())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bare internbrukere tillatt");
    }

    @PutMapping("/henvendelse/{henvendelseId}/kasser")
    public int kasserHenvendelse(@PathVariable String henvendelseId) {
        sjekkErInternbruker();

        long id = Long.parseLong(henvendelseId);
        DialogData dialogData = dialogDAO.hentDialogGittHenvendelse(id);

        return kjorHvisTilgang(AktorId.of(dialogData.getAktorId()), "henvendelse", henvendelseId, () -> dialogDAO.kasserHenvendelse(id));
    }

    @PutMapping("/dialog/{dialogId}/kasser")
    @Transactional
    public int kasserDialog(@PathVariable String dialogId) {
        sjekkErInternbruker();

        long id = Long.parseLong(dialogId);
        DialogData dialogData = dialogDAO.hentDialog(id);

        return kjorHvisTilgang(AktorId.of(dialogData.getAktorId()), "dialog", dialogId, () -> {
            int antallHenvendelser = dialogData.getHenvendelser()
                    .stream()
                    .mapToInt(henvendelse -> dialogDAO.kasserHenvendelse(henvendelse.id))
                    .sum();

            return dialogDAO.kasserDialog(id) + antallHenvendelser;
        });
    }

    private int kjorHvisTilgang(AktorId aktorId, String kasseringAv, String id, IntSupplier oppdaterteDialoger) {
        Id veilederIdent = auth.getInnloggetVeilederIdent();
        auth.sjekkTilgangTilPerson(aktorId);
        List<String> godkjente = Arrays.asList(godkjenteIdenter.split(","));
        if (!godkjente.contains(veilederIdent.get())) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} dialoger", veilederIdent, aktorId);
            throw new ResponseStatusException(FORBIDDEN,
                    String.format("[KASSERING] %s har ikke tilgang til kassinger av %s dialoger", veilederIdent, aktorId));
        }

        int updated = oppdaterteDialoger.getAsInt();

        log.info("[KASSERING] {} kasserte en {}. AktoerId: {} {}_id: {}", veilederIdent, kasseringAv, aktorId, kasseringAv, id);
        return updated;
    }
}
