package no.nav.fo.veilarbdialog.brukernotifikasjon;

import no.nav.common.types.identer.Fnr;

import java.net.URL;
import java.util.UUID;

public record Brukernotifikasjon(
        UUID brukernotifikasjonId,
        long dialogId,
        Fnr foedselsnummer,
        String melding,
        UUID oppfolgingsperiode,
        VarselType type,
        String epostTitel,
        String epostBody,
        String smsTekst,
        URL link
) {
}
