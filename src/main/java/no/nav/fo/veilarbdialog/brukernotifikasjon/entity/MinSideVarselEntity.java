package no.nav.fo.veilarbdialog.brukernotifikasjon.entity;

import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType;
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

public record MinSideVarselEntity(
        MinSideVarselId varselId,
        Fnr fnr,
        UUID oppfolgingsPeriodeId,
        BrukernotifikasjonsType type,
        BrukernotifikasjonBehandlingStatus status,
        VarselKvitteringStatus varselKvitteringStatus,
        LocalDateTime opprettet,
        LocalDateTime varselFeilet,
        LocalDateTime avsluttet,
        LocalDateTime bekreftetSendt,
        LocalDateTime forsoktSendt,
        LocalDateTime ferdigBehandlet,
        String melding,
        URL lenke,
        Boolean skalBatches
) {
}
