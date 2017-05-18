package no.nav.fo.veilarbdialog.api;

import no.nav.fo.veilarbdialog.domain.DialogAktorDTO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Date;
import java.util.List;


@Path("/dialog")
public interface DialogAktorController {

    @GET
    @Path("/aktor/feed/{tidspunkt}")
    List<DialogAktorDTO> hentAktorerMedEndringerEtter(@PathParam("tidspunkt") Date tidspunkt);

}
