package no.nav.fo.veilarbdialog.eskaleringsvarsel.dto;

import java.time.ZonedDateTime;

public record EskaleringsvarselDto(
        long id,
        long tilhorendeDialogId,

        String opprettetAv,
        ZonedDateTime opprettetDato,
        String opprettetBegrunnelse,

        ZonedDateTime avsluttetDato,
        String avsluttetAv,
        String avsluttetBegrunnelse
) {
}
