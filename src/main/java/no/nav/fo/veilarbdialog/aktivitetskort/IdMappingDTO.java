package no.nav.fo.veilarbdialog.aktivitetskort;

import java.util.UUID;

public record IdMappingDTO(
    String arenaId,
    Long aktivitetId,
    UUID funksjonellId
) {
}
