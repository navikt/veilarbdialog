package no.nav.fo.veilarbdialog.brukernotifikasjon;

import no.nav.common.types.identer.Fnr;

import java.net.URL;
import java.util.UUID;

public record Brukernotifikasjon(
        UUID eventId,
        long dialogId,
        Fnr foedselsnummer,
        String melding,
        UUID oppfolgingsperiodeId,
        VarselType type,
        String epostTitel,
        String epostBody,
        String smsTekst,
        URL link
) {
}
