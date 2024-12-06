package no.nav.fo.veilarbdialog.eskaleringsvarsel.entity;

import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record EskaleringsvarselEntity(
        long varselId,
        long tilhorendeDialogId,
        long tilhorendeBrukernotifikasjonId,
        MinSideVarselId tilhorendeVarselId,
        String aktorId,
        String opprettetAv,
        ZonedDateTime opprettetDato,
        String opprettetBegrunnelse,
        ZonedDateTime avsluttetDato,
        String avsluttetAv,
        String avsluttetBegrunnelse,
        UUID oversiktenSendingUuid
) {
}
