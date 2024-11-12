package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder
public class DoneInfo {
    String eventId;
}
