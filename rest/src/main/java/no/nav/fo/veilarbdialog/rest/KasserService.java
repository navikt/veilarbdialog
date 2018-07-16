package no.nav.fo.veilarbdialog.rest;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import java.util.Arrays;
import java.util.List;

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

    @Value("${veilarb.kassering.identer:Z999999}")
    String godkjenteIdenter;

    @PUT
    @Path("/henvendelse/{id}/kasser")
    public int kasserHenvendelse(@Context Request request, @PathParam("id") String henvendelseId) {
        long id = Long.parseLong(henvendelseId);
        DialogData dialogData = dialogDAO.hentDialogGittHenvendelse(id);
        sjekkTilgang(dialogData.getAktorId(), "henvendelse", henvendelseId);

        return dialogDAO.kasserHenvendelse(id);
    }

    @PUT
    @Path("/dialog/{id}/kasser")
    public int kasserDialog(@PathParam("id") String dialogId) {
        long id = Long.parseLong(dialogId);
        DialogData dialogData = dialogDAO.hentDialog(id);
        sjekkTilgang(dialogData.getAktorId(), "dialog", dialogId);

        int antallHenvendelser = dialogData.getHenvendelser()
                .stream()
                .mapToInt((henvendelse) -> dialogDAO.kasserHenvendelse(henvendelse.id))
                .sum();

        return dialogDAO.kasserDialog(id) + antallHenvendelser;
    }

    private void sjekkTilgang(String aktorId, String kasseringAv, String id) {
        String fnr = aktorService.getFnr(aktorId).orElseThrow(IngenTilgang::new);
        pep.sjekkLeseTilgangTilFnr(fnr);

        String veilederIdent = SubjectHandler.getIdent().orElse(null);
        List<String> godkjente = Arrays.asList(godkjenteIdenter.split("[\\.\\s]"));
        if (!godkjente.contains(veilederIdent)) {
            log.error("[KASSERING] {} har ikke tilgang til kassering av {} dialoger", veilederIdent, aktorId);
            throw new IngenTilgang(String.format("[KASSERING] %s har ikke tilgang til kassinger av %s dialoger", veilederIdent, aktorId));
        }
        log.info("[KASSERING] {} kasserte en {}. AktoerId: {} {}_id: {}", veilederIdent, kasseringAv, aktorId, kasseringAv, id);
    }
}
