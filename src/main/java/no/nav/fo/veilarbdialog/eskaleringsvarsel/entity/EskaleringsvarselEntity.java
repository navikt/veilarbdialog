package no.nav.fo.veilarbdialog.eskaleringsvarsel.entity;

import java.time.ZonedDateTime;

public record EskaleringsvarselEntity(
        long varselId,
        long tilhorendeDialogId,
        long tilhorendeBrukernotifikasjonId,
        String aktorId,
        String opprettetAv,
        ZonedDateTime opprettetDato,
        String opprettetBegrunnelse,
        ZonedDateTime avsluttetDato,
        String avsluttetAv,
        String avsluttetBegrunnelse
) {
}
