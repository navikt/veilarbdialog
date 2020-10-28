package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.types.feil.IngenTilgang;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.service.AuthService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.fo.veilarbdialog.config.ApplicationConfig.VEILARB_KASSERING_IDENTER_PROPERTY;

@RestController
@RequestMapping("/api/kassering")
@RequiredArgsConstructor
@Slf4j
public class KasserRessurs {

    private final DialogDAO dialogDAO;
    private final Pep pep;
    private final AuthService auth;

    private final String godkjenteIdenter = EnvironmentUtils.getOptionalProperty(VEILARB_KASSERING_IDENTER_PROPERTY).orElse("");

    @PutMapping("/henvendelse/{henvendelseId}/kasser")
    public int kasserHenvendelse(@PathVariable String henvendelseId) {
        auth.skalVereInternBruker();

        long id = Long.parseLong(henvendelseId);
        DialogData dialogData = dialogDAO.hentDialogGittHenvendelse(id);

        return kjorHvisTilgang(dialogData.getAktorId(), "henvendelse", henvendelseId, () -> dialogDAO.kasserHenvendelse(id));
    }

    @PutMapping("/dialog/{dialogId}/kasser")
    @Transactional
    public int kasserDialog(@PathVariable String dialogId) {
        auth.skalVereInternBruker();

        long id = Long.parseLong(dialogId);
        DialogData dialogData = dialogDAO.hentDialog(id);

        return kjorHvisTilgang(dialogData.getAktorId(), "dialog", dialogId, () -> {
            int antallHenvendelser = dialogData.getHenvendelser()
                    .stream()
                    .mapToInt((henvendelse) -> dialogDAO.kasserHenvendelse(henvendelse.id))
                    .sum();

            return dialogDAO.kasserDialog(id) + antallHenvendelser;
        });
    }

    private int kjorHvisTilgang(String aktorId, String kasseringAv, String id, Supplier<Integer> fn) {

        String veilederIdent = auth.getIdent().orElse(null);
        if (!auth.identifiedUserHasReadAccessToPerson(veilederIdent, aktorId)) {
            throw new IngenTilgang(String.format(
                    "%s does not have read access to %s",
                    veilederIdent,
                    aktorId
            ));
        }
        List<String> godkjente = Arrays.asList(godkjenteIdenter.split(","));
        if (!godkjente.contains(veilederIdent)) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} dialoger", veilederIdent, aktorId);
            throw new IngenTilgang(String.format("[KASSERING] %s har ikke tilgang til kassinger av %s dialoger", veilederIdent, aktorId));
        }

        int updated = fn.get();

        log.info("[KASSERING] {} kasserte en {}. AktoerId: {} {}_id: {}", veilederIdent, kasseringAv, aktorId, kasseringAv, id);
        return updated;
    }
}
