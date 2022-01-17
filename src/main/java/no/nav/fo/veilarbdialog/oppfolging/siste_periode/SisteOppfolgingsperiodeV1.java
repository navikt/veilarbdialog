package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Data
@With
public class SisteOppfolgingsperiodeV1 {
    UUID uuid;
    String aktorId;
    ZonedDateTime startDato;
    ZonedDateTime sluttDato;
}
