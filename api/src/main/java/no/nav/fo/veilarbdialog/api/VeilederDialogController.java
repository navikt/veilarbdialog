package no.nav.fo.veilarbdialog.api;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.OppdaterDialogDTO;

import javax.ws.rs.*;
import java.util.List;


@Path("/dialog")
public interface VeilederDialogController {

    @PUT
    @Path("/{id}")
    DialogDTO oppdaterDialog(OppdaterDialogDTO dialogDTO);

}
