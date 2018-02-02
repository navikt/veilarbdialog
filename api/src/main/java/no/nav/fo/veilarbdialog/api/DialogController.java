package no.nav.fo.veilarbdialog.api;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.NyHenvendelseDTO;

import javax.ws.rs.*;
import java.util.List;


@Path("/dialog")
public interface DialogController {

    @GET
    List<DialogDTO> hentDialoger();

    @GET
    @Path("/{fnr}")
    List<DialogDTO> hentDialogerForBruker(@PathParam("fnr") String fnr);

    @POST
    DialogDTO nyHenvendelse(NyHenvendelseDTO nyHenvendelseDTO);

    @POST
    @Path("/{fnr}")
    DialogDTO nyHenvendelseForBruker(@PathParam("fnr") String fnr, NyHenvendelseDTO nyHenvendelseDTO);

    @PUT
    @Path("/{dialogId}/les")
    DialogDTO markerSomLest(@PathParam("dialogId") String dialogId);

}
