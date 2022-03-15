package no.nav.fo.veilarbdialog.brukernotifikasjon.entity;

import java.util.UUID;

public record BrukernotifikasjonEntity(
        long id,
        UUID eventId,
        UUID oppfolgingsPeriodeId
) {
}
