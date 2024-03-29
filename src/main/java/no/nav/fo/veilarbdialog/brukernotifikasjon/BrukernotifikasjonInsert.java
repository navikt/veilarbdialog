package no.nav.fo.veilarbdialog.brukernotifikasjon;

import no.nav.common.types.identer.Fnr;

import java.net.URL;
import java.util.UUID;

public record BrukernotifikasjonInsert(
        UUID eventId,
        long dialogId,
        Fnr foedselsnummer,
        String melding,
        UUID oppfolgingsperiodeId,
        BrukernotifikasjonsType type,
        BrukernotifikasjonBehandlingStatus status,
        URL link) {
}
