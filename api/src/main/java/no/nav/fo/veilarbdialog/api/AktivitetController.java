package no.nav.fo.veilarbdialog.api;

import no.nav.fo.veilarbdialog.domain.AktivitetDTO;
import no.nav.fo.veilarbdialog.domain.AktivitetsplanDTO;
import no.nav.fo.veilarbdialog.domain.EndringsloggDTO;
import no.nav.fo.veilarbdialog.domain.EtikettType;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Etikett;

import javax.ws.rs.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Path("/aktivitet")
public interface AktivitetController {

    @GET
    AktivitetsplanDTO hentAktivitetsplan();

    @POST
    @Path("/ny")
    AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet);

    @PUT
    @Path("/{id}")
    AktivitetDTO oppdaterAktiviet(AktivitetDTO aktivitet);

    @GET
    @Path("/etiketter")
    default List<EtikettType> hentEtiketter(){
        return Arrays.stream(Etikett.values())
                .map(EtikettType::fraEtikett)
                .collect(Collectors.toList());
    }

    @DELETE
    @Path("/{id}")
    void slettAktivitet(@PathParam("id") String id);

    @PUT
    @Path("/{id}/status/{status}")
    AktivitetDTO oppdaterStatus(@PathParam("id") String aktivitetId, @PathParam("status") String status);

    @GET
    @Path("/{id}/endringslogg")
    List<EndringsloggDTO> hentEndringsLoggForAktivitetId(@PathParam("id") String aktivitetId);

}
