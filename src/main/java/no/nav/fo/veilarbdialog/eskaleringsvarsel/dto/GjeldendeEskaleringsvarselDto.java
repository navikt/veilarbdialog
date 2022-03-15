package no.nav.fo.veilarbdialog.eskaleringsvarsel.dto;

import java.time.ZonedDateTime;

public record GjeldendeEskaleringsvarselDto(
        long id,
        long tilhorendeDialogId,

        String opprettetAv,
        ZonedDateTime opprettetDato,
        String opprettetBegrunnelse
) {
}
