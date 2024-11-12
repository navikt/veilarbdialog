package no.nav.fo.veilarbdialog.brukernotifikasjon;

import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;

import java.net.URL;
import java.util.UUID;

public record BrukernotifikasjonInsert(
        MinSideVarselId varselId,
        long dialogId,
        Fnr foedselsnummer,
        String melding,
        UUID oppfolgingsperiodeId,
        BrukernotifikasjonsType type,
        BrukernotifikasjonBehandlingStatus status,
        URL link) {
}
