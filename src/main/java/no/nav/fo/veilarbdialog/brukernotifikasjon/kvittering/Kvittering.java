package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import java.time.LocalDateTime;

public record Kvittering (
        LocalDateTime tidspunkt,
        String brukernotifikasjonBestillingId,
        String doknotifikasjonStatus,
        String melding,
        Long distribusjonId
) {}
