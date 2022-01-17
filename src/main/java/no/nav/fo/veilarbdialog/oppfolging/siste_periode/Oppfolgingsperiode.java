package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import java.time.ZonedDateTime;
import java.util.UUID;

record Oppfolgingsperiode(String aktorid, UUID oppfolgingsperiode, ZonedDateTime startTid,
                                 ZonedDateTime sluttTid) {
}
