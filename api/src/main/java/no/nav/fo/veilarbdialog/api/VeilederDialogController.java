package no.nav.fo.veilarbdialog.api;

import no.nav.fo.veilarbdialog.domain.DialogDTO;

import javax.ws.rs.*;


@Path("/dialog")
public interface VeilederDialogController {

    @PUT
    @Path("/{id}/venter_pa_svar/{venter}")
    DialogDTO oppdaterVenterPaSvar(@PathParam("id") String dialogId, @PathParam("venter") boolean venter);

    @PUT
    @Path("/{id}/ferdigbehandlet/{ferdigbehandlet}")
    DialogDTO oppdaterFerdigbehandlet(@PathParam("id") String dialogId, @PathParam("ferdigbehandlet") boolean ferdigbehandlet);

}
