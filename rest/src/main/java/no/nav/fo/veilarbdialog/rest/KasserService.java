package no.nav.fo.veilarbdialog.rest;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.service.AutorisasjonService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@Path("/kassering")
public class KasserService {

    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private PepClient pep;

    @Inject
    private AktorService aktorService;

    @Value("${veilarb.kassering.identer:Z990322}")
    String godkjenteIdenter;

    @Inject
    private AutorisasjonService autorisasjonService;

    @PUT
    @Path("/henvendelse/{id}/kasser")
    public int kasserHenvendelse(@Context Request request, @PathParam("id") String henvendelseId) {
        autorisasjonService.skalVereInternBruker();

        long id = Long.parseLong(henvendelseId);
        DialogData dialogData = dialogDAO.hentDialogGittHenvendelse(id);

        return kjorHvisTilgang(dialogData.getAktorId(), "henvendelse", henvendelseId, () -> dialogDAO.kasserHenvendelse(id));
    }

    @PUT
    @Path("/dialog/{id}/kasser")
    @Transactional
    public int kasserDialog(@PathParam("id") String dialogId) {
        autorisasjonService.skalVereInternBruker();

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
        String fnr = aktorService.getFnr(aktorId).orElseThrow(IngenTilgang::new);
        pep.sjekkLeseTilgangTilFnr(fnr);

        String veilederIdent = SubjectHandler.getIdent().orElse(null);
        List<String> godkjente = Arrays.asList(godkjenteIdenter.split("[\\.\\s]"));
        if (!godkjente.contains(veilederIdent)) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} dialoger", veilederIdent, aktorId);
            throw new IngenTilgang(String.format("[KASSERING] %s har ikke tilgang til kassinger av %s dialoger", veilederIdent, aktorId));
        }

        int updated = fn.get();

        log.info("[KASSERING] {} kasserte en {}. AktoerId: {} {}_id: {}", veilederIdent, kasseringAv, aktorId, kasseringAv, id);
        return updated;
    }
}
