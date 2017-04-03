package no.nav.fo.veilarbdialog.api;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyDialogDTO;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;


@Path("/dialog")
public interface DialogController {

    @GET
    List<DialogDTO> hentDialogerForBruker();

    @POST
    DialogDTO opprettDialogForAktivitetsplan(NyDialogDTO nyDialogDTO);

}
