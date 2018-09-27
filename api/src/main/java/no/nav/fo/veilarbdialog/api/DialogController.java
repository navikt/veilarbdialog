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
    @Path("/{dialogId}")
    DialogDTO hentDialog(@PathParam("dialogId") String dialogId);

    @POST
    DialogDTO nyHenvendelse(NyHenvendelseDTO nyHenvendelseDTO);

    @POST
    @Path("/forhandsorientering")
    DialogDTO forhandsorienteringPaAktivitet(NyHenvendelseDTO nyHenvendelseDTO);

    @PUT
    @Path("/{dialogId}/les")
    DialogDTO markerSomLest(@PathParam("dialogId") String dialogId);

}
