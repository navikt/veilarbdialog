package no.nav.fo.veilarbdialog.clients.veilarbperson;

import no.nav.common.types.identer.Fnr;

import java.util.Optional;

public interface VeilarbpersonClient {

    Optional<Nivaa4DTO> hentNiva4(Fnr fnr);

}
