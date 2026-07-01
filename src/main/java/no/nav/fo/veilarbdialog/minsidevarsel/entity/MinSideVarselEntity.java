package no.nav.fo.veilarbdialog.minsidevarsel.entity;

import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus;
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselType;
import no.nav.fo.veilarbdialog.minsidevarsel.VarselKvitteringStatus;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

public record MinSideVarselEntity(
        MinSideVarselId varselId,
        Fnr fnr,
        UUID oppfolgingsPeriodeId,
        MinSideVarselType type,
        MinSideVarselBehandlingStatus status,
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
