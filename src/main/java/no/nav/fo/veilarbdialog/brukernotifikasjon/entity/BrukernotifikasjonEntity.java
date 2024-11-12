package no.nav.fo.veilarbdialog.brukernotifikasjon.entity;

import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType;
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

public record BrukernotifikasjonEntity(
        long id,
        UUID varselId,
        long dialogId,
        Fnr fnr,
        UUID oppfolgingsPeriodeId,
        BrukernotifikasjonsType type,
        BrukernotifikasjonBehandlingStatus status,
        VarselKvitteringStatus varselKvitteringStatus,
        LocalDateTime opprettet,
        LocalDateTime forsoktSendt,
        String melding,
        String smsText,
        String epostTittel,
        String epostBody,
        URL lenke
) {
}
